package com.miguel.backend_for_front.favorite;

import com.miguel.backend_for_front.ingredient.FoodType;
import com.miguel.backend_for_front.ingredient.Ingredient;
import com.miguel.backend_for_front.ingredient.IngredientRepository;
import com.miguel.backend_for_front.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock private FavoriteRepository favoriteRepository;
    @Mock private IngredientRepository ingredientRepository;

    @InjectMocks
    private FavoriteService favoriteService;

    private User user;
    private Ingredient ingredient;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("u@test.com").build();
        ingredient = Ingredient.builder().id(10L).name("Chicken").foodType(FoodType.MEAT).build();
    }

    @Test
    void addFavorite_savesWhenNotYetFavorited() {
        when(ingredientRepository.findById(10L)).thenReturn(Optional.of(ingredient));
        when(favoriteRepository.existsByUserAndIngredient(user, ingredient)).thenReturn(false);

        favoriteService.addFavorite(user, 10L);

        verify(favoriteRepository).save(any(Favorite.class));
    }

    @Test
    void addFavorite_isIdempotentWhenAlreadyFavorited() {
        when(ingredientRepository.findById(10L)).thenReturn(Optional.of(ingredient));
        when(favoriteRepository.existsByUserAndIngredient(user, ingredient)).thenReturn(true);

        favoriteService.addFavorite(user, 10L);

        verify(favoriteRepository, never()).save(any());
    }


    @Test
    void addFavorite_throwsWhenIngredientNotFound() {
        when(ingredientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoriteService.addFavorite(user, 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ingredient not found");
    }

    @Test
    void removeFavorite_deletesTheFavorite() {
        when(ingredientRepository.findById(10L)).thenReturn(Optional.of(ingredient));

        favoriteService.removeFavorite(user, 10L);

        verify(favoriteRepository).deleteByUserAndIngredient(user, ingredient);
    }
}