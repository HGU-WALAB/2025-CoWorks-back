package com.hiswork.backend.dto;

import com.hiswork.backend.domain.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private UUID id;
    private String email;
    private String name;
    private String position;
    private String role;
    private String token;
    
    public static AuthResponse from(User user, String token) {
        return AuthResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .position(user.getPosition() != null ? user.getPosition().name() : null)
                .role(user.getRole().name())
                .token(token)
                .build();
    }
} 