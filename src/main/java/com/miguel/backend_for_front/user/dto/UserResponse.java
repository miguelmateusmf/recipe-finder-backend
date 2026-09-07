package com.miguel.backend_for_front.user.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
}
