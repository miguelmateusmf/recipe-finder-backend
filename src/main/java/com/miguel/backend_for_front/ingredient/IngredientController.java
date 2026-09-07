package com.miguel.backend_for_front.ingredient;

import com.miguel.backend_for_front.ingredient.dto.IngredientResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ingredients")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientService ingredientService;

    @GetMapping
    public List<IngredientResponse> getIngredients(
            @RequestParam(required = false) FoodType foodType,
            @RequestParam(required = false) String search) {

        if (search != null && !search.isBlank()) {
            return ingredientService.search(search);
        }
        if (foodType != null) {
            return ingredientService.getByFoodType(foodType);
        }
        return ingredientService.getAll();
    }

}