package com.miguel.backend_for_front.favorite;

import com.miguel.backend_for_front.favorite.dto.FavoriteResponse;
import com.miguel.backend_for_front.ingredient.Ingredient;
import com.miguel.backend_for_front.ingredient.IngredientRepository;
import com.miguel.backend_for_front.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final IngredientRepository ingredientRepository;

    public List<FavoriteResponse> getFavorites(User user) {
        return favoriteRepository.findByUser(user).stream()
                .map(fav -> FavoriteResponse.builder()
                        .ingredientId(fav.getIngredient().getId())
                        .name(fav.getIngredient().getName())
                        .foodType(fav.getIngredient().getFoodType())
                        .build())
                .toList();
    }

    public List<Long> getFavoriteIds(User user) {
        return favoriteRepository.findIngredientIdsByUser(user);
    }

    @Transactional
    public void addFavorite(User user, Long ingredientId) {
        Ingredient ingredient = ingredientRepository.findById(ingredientId)
                .orElseThrow(() -> new IllegalArgumentException("Ingredient not found"));

        if (favoriteRepository.existsByUserAndIngredient(user, ingredient)) {
            return; // already favorited, idempotent — no error
        }

        Favorite favorite = Favorite.builder()
                .user(user)
                .ingredient(ingredient)
                .build();
        favoriteRepository.save(favorite);
    }

    @Transactional
    public void removeFavorite(User user, Long ingredientId) {
        Ingredient ingredient = ingredientRepository.findById(ingredientId)
                .orElseThrow(() -> new IllegalArgumentException("Ingredient not found"));
        favoriteRepository.deleteByUserAndIngredient(user, ingredient);
    }
}