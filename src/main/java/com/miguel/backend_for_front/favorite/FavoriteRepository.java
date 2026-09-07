package com.miguel.backend_for_front.favorite;

import com.miguel.backend_for_front.ingredient.Ingredient;
import com.miguel.backend_for_front.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findByUser(User user);
//delete this
    Optional<Favorite> findByUserAndIngredient(User user, Ingredient ingredient);

    boolean existsByUserAndIngredient(User user, Ingredient ingredient);

    void deleteByUserAndIngredient(User user, Ingredient ingredient);

    // Just the ingredient IDs a user has favorited — lighter than loading full Favorites
    @Query("SELECT f.ingredient.id FROM Favorite f WHERE f.user = :user")
    List<Long> findIngredientIdsByUser(User user);
}