# Recipe Finder — Backend

A Spring Boot REST API powering the [Recipe Finder](https://recipe-finder.recipes) app. Handles authentication, user management, ingredients, and favorites.

**Live app:** [https://recipe-finder.recipes](https://recipe-finder.recipes)
**Frontend repo:** [recipe-finder-frontend](https://github.com/YOUR-USERNAME/recipe-finder-frontend)

---

## Stack

**Framework & runtime**
- Java 21
- Spring Boot 4
- Maven

**Data**
- Spring Data JPA + Hibernate 7
- PostgreSQL 17 (RDS in production, local Postgres for dev)
- HikariCP connection pool

**Security**
- Spring Security 6
- JWT authentication (jjwt library)
- BCrypt password hashing
- Bucket4j rate limiting

**Testing**
- JUnit 5 + Mockito (service unit tests)
- Spring MockMvc (controller integration tests)
- Testcontainers (real Postgres in integration tests)

**Containerization**
- Multi-stage Dockerfile (Maven build → JRE runtime, ~200MB image)

---

## Architectural decisions

### Modular monolith

The app is a single Spring Boot service organized by feature package (`auth`, `user`, `ingredient`, `favorite`).

### Feature-based package structure

```
com.miguel.backend_for_front/
├── auth/          (AuthController, AuthService, JwtService, filters, DTOs)
├── user/          (UserController, UserService, User entity, Role enum, DTOs)
├── ingredient/    (IngredientController, IngredientService, FoodType enum)
├── favorite/      (FavoriteController, FavoriteService, race-safe logic)
├── config/        (SecurityConfig, CorsConfig, RateLimitFilter)
└── exception/     (ApiError, GlobalExceptionHandler)
```

Each feature owns its own DTOs, entities, and services. Cross-cutting concerns (security, CORS, exception handling) live in `config` and `exception`.

### ID-based JWT subject

- **Email changes don't invalidate live sessions.** The token identifies the user immutably.
- **User lookups on request are indexed** (`findById` on a primary key) rather than a secondary lookup on email.

### Two-layer error handling

Errors are handled at two distinct layers:

1. **`GlobalExceptionHandler`** (`@RestControllerAdvice`) — catches business/validation exceptions from controllers, returns structured `ApiError` JSON.
2. **`JwtAuthenticationEntryPoint`** — catches security-level failures (missing/expired/invalid tokens) that never reach a controller. Writes the same `ApiError` shape.

The auth filter tags requests with an `auth_error` attribute (`expired`, `invalid`, `missing`); the entry point reads it and returns a specific message instead of a generic 401. Uniform error shape across the API, whether the failure was authorization or business logic.

### Optimistic favorites with race-condition handling

Favoriting is idempotent. `addFavorite` first checks `existsByUserAndIngredient`; if the row exists, it returns quietly. If two requests race past the check and both attempt to insert, the database's unique constraint on `(user_id, ingredient_id)` prevents the duplicate. The service catches `DataIntegrityViolationException` and swallows it — the desired state (favorited) is achieved either way.

The exists-check is an optimization to avoid the exception in the common case; the unique constraint is the real guarantee. This is tested with a Mockito test that forces the exception path and asserts the service doesn't propagate it.

### JWT filter and Spring Security integration

The custom `JwtAuthenticationFilter` extends `OncePerRequestFilter`, extracts the bearer token, validates it, loads the user by ID, and sets the security context. It uses `SecurityContextHolderStrategy` (not the static holder) for testability, and catches `ExpiredJwtException` and `JwtException` separately so error responses can distinguish between "expired token" and "invalid token" without dumping stack traces.

Registered in the security filter chain before `UsernamePasswordAuthenticationFilter`.

### Rate limiting on auth endpoints

`/auth/login` and `/auth/register` are rate-limited via Bucket4j: 5 requests per minute per IP. Implemented as a filter running *before* the JWT filter so blocked requests never reach auth logic. Buckets are stored in a `ConcurrentHashMap` keyed by IP.

Known limitations, documented as production upgrade paths:
- **In-memory buckets** don't survive restarts and don't scale across instances — production would use `bucket4j-redis` with a shared Redis backend.
- **`getRemoteAddr()`** returns the load balancer's IP if behind a proxy — production would read `X-Forwarded-For` (only from trusted proxies).

Returns 429 with the same `ApiError` shape as other errors.

### Password verification for sensitive updates

Changing email or password requires the current password, verified via BCrypt in `UserService`. Prevents an attacker with a stolen session from taking over the account by changing the email. The check is at the service layer, tested with Mockito.

### CORS via environment-driven allowed origins

`CorsConfig` reads allowed origins from a constant list (portfolio scope). In real production this would move to an environment variable so origins change without a code rebuild. The setup keeps localhost origins for dev alongside the deployed frontend origin.

---

## Testing

**Service unit tests** (JUnit 5 + Mockito)

Each service tested in isolation with mocked repositories and encoder.

- `AuthServiceTest` — register happy path (user persisted with hashed password, role set, token returned); duplicate email throws; login delegates to `AuthenticationManager`; bad credentials propagate.
- `FavoriteServiceTest` — idempotent add (no double save); race condition swallowed (DataIntegrityViolationException caught); missing ingredient throws; remove deletes.
- `UserServiceTest` — password change verifies current password; email uniqueness enforced with "allow keeping your own email" edge case.

**Controller integration test** (Spring MockMvc + Testcontainers)

`AuthControllerIntegrationTest` boots the full Spring context against a real Postgres 17 container (via Testcontainers), then drives the API through MockMvc:

- Register endpoint returns 200 with a token
- Invalid input returns 400 (via `GlobalExceptionHandler`)
- Protected endpoint without token returns 401 (via `JwtAuthenticationEntryPoint`)

Tests the entire security filter chain end-to-end. Postgres in a container matches production behavior (dialects, JSON operators, sequence generation) — closer to reality than H2.

Uses Spring Boot's `@ServiceConnection` to auto-wire the container's connection details, so no property overrides needed.

---

## Local development

Requires:
- Java 21
- Maven 3.9+
- Docker Desktop (for Testcontainers integration tests)
- PostgreSQL 17 running locally (or use `docker run postgres:17`)

```bash
# Set env vars (via IDE run config)
export DB_URL=jdbc:postgresql://url
export DB_USERNAME=example
export DB_PASSWORD=example
export JWT_SECRET=64-plus-character-chain-here

# Build and run
./mvnw clean package
./mvnw spring-boot:run

# Or run tests
./mvnw test
```

App runs on `http://localhost:8080`. See `.env.example` for the full list of required variables.

---

## AWS deployment

The API runs on AWS with HTTPS end-to-end, a custom domain, and managed Postgres.

### Architecture

```
User browser
    ↓ HTTPS
CloudFront (frontend)
    ↓
S3 (static files)

Frontend JS calls:
    ↓ HTTPS
api.recipe-finder.recipes
    ↓ (Route 53 alias)
Application Load Balancer ── SSL cert (ACM eu-west-1)
    ↓ HTTP (within VPC)
ECS Fargate task (Spring Boot container)
    ↓
RDS Postgres 17
```

## API

Base URL: `https://api.recipe-finder.recipes`

**Public endpoints:**
- `POST /auth/register` — create account, returns JWT
- `POST /auth/login` — authenticate, returns JWT

**Protected endpoints** (require `Authorization: Bearer <token>`):
- `GET /api/users/me` — current user profile
- `PATCH /api/users/me` — update name
- `PATCH /api/users/me/email` — update email (requires current password)
- `PATCH /api/users/me/password` — change password (requires current password)
- `GET /api/ingredients` — list all ingredients
- `GET /api/favorites/ids` — favorite ingredient IDs for the current user
- `POST /api/favorites/{id}` — favorite an ingredient (idempotent)
- `DELETE /api/favorites/{id}` — unfavorite an ingredient

Errors return a uniform `ApiError` shape:
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Token has expired",
  "path": "/api/users/me"
}
```

---

## Future work

Deferred consciously for MVP scope:

- **Refresh tokens** — currently a single JWT with 24h expiry. Production would use short-lived (15 min) access tokens + long-lived refresh tokens stored securely, rotated on use.
- **Distributed rate limiting** — Bucket4j buckets are in-memory. Horizontal scaling would need Redis-backed buckets via `bucket4j-redis`.
- **AWS Secrets Manager** — task environment variables hold secrets in plaintext. Production would pull from Secrets Manager and rotate periodically.
- **Ingredient translations table** — currently done frontend-side via a static map. A production system would model `ingredient_translations (ingredient_id, language, name)` to scale beyond two languages.
- **Restrictions / allergies** — planned feature; would add a `user_restrictions` table and filter ingredients accordingly on the API side.
- **CI/CD** — GitHub Actions pipeline for build → test → push to ECR → force ECS deployment.
- **CORS from env var** — moving allowed origins to a task environment variable so URL changes don't require a rebuild.
- **Observability** — CloudWatch logs are enabled but no dashboards, metrics, or alarms. Production would add structured logging, tracing (X-Ray), and alerts on 5xx spikes or health check failures.
