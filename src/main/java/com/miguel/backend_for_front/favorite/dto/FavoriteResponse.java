package com.miguel.backend_for_front.favorite.dto;

import com.miguel.backend_for_front.ingredient.FoodType;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FavoriteResponse {
    private Long ingredientId;
    private String name;
    private FoodType foodType;
}