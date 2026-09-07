package com.miguel.backend_for_front.ingredient;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
    List<Ingredient> findByFoodType(FoodType foodType);
    List<Ingredient> findByNameContainingIgnoreCase(String name);
}