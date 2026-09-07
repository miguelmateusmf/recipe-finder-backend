package com.miguel.backend_for_front.ingredient.dto;

import com.miguel.backend_for_front.ingredient.FoodType;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IngredientResponse {
    private Long id;
    private String name;
    private FoodType foodType;
}