package com.miguel.backend_for_front.favorite;

import com.miguel.backend_for_front.favorite.dto.FavoriteResponse;
import com.miguel.backend_for_front.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    // Full favorite ingredients (for the favorites list view)
    @GetMapping
    public List<FavoriteResponse> getFavorites(@AuthenticationPrincipal User user) {
        return favoriteService.getFavorites(user);
    }

    // Just the IDs (for rendering filled hearts across the main list)
    @GetMapping("/ids")
    public List<Long> getFavoriteIds(@AuthenticationPrincipal User user) {
        return favoriteService.getFavoriteIds(user);
    }

    @PostMapping("/{ingredientId}")
    public ResponseEntity<Void> addFavorite(
            @AuthenticationPrincipal User user,
            @PathVariable Long ingredientId) {
        favoriteService.addFavorite(user, ingredientId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{ingredientId}")
    public ResponseEntity<Void> removeFavorite(
            @AuthenticationPrincipal User user,
            @PathVariable Long ingredientId) {
        favoriteService.removeFavorite(user, ingredientId);
        return ResponseEntity.noContent().build();
    }
}