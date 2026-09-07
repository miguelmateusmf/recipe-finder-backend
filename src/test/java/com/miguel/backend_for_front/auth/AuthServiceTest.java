package com.miguel.backend_for_front.auth;

import com.miguel.backend_for_front.auth.dto.LoginRequest;
import com.miguel.backend_for_front.auth.dto.RegisterRequest;
import com.miguel.backend_for_front.user.Role;
import com.miguel.backend_for_front.user.User;
import com.miguel.backend_for_front.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest validRegister;

    @BeforeEach
    void setUp() {
        validRegister = RegisterRequest.builder()
                .email("new@test.com")
                .password("password123")
                .firstName("Test")
                .lastName("User")
                .build();
    }

    @Test
    void register_persistsUserAndReturnsToken() {
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        var response = authService.register(validRegister);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        verify(userRepository).save(argThat(u ->
                u.getEmail().equals("new@test.com") &&
                        u.getPassword().equals("hashed") &&
                        u.getRole() == Role.USER
        ));
    }

    @Test
    void register_throwsWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail("new@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(validRegister))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_returnsTokenForValidCredentials() {
        User user = User.builder()
                .id(1L).email("test@test.com").password("hashed")
                .firstName("T").lastName("U").role(Role.USER).build();

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        var response = authService.login(new LoginRequest("test@test.com", "password123"));

        assertThat(response.getToken()).isEqualTo("jwt-token");
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void login_throwsWhenAuthenticationFails() {
        doThrow(new BadCredentialsException("bad"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() ->
                authService.login(new LoginRequest("test@test.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }
}