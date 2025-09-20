package com.hiswork.backend.dto;

import com.hiswork.backend.domain.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignUpResponse {
    private String id;
    private String email;
    private String name;
    private String position;
    private String role;
    private String token;

    public static SignUpResponse from(User user, String token) {
        return SignUpResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .position(user.getPosition() != null ? user.getPosition().name() : null)
                .role(user.getRole() != null ? user.getRole().name() : null)
                .token(token)
                .build();
    }
}
