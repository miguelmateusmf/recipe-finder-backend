package com.miguel.backend_for_front.ingredient;

import com.miguel.backend_for_front.ingredient.FoodType;
import com.miguel.backend_for_front.ingredient.Ingredient;
import com.miguel.backend_for_front.ingredient.IngredientRepository;
import com.miguel.backend_for_front.ingredient.dto.IngredientResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IngredientService {

    private final IngredientRepository ingredientRepository;

    public List<IngredientResponse> getAll() {
        return ingredientRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<IngredientResponse> getByFoodType(FoodType foodType) {
        return ingredientRepository.findByFoodType(foodType).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<IngredientResponse> search(String query) {
        return ingredientRepository.findByNameContainingIgnoreCase(query).stream()
                .map(this::toResponse)
                .toList();
    }

    private IngredientResponse toResponse(Ingredient ingredient) {
        return IngredientResponse.builder()
                .id(ingredient.getId())
                .name(ingredient.getName())
                .foodType(ingredient.getFoodType())
                .build();
    }
}