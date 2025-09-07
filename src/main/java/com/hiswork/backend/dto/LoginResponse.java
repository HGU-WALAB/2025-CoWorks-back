package com.hiswork.backend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {
    private String token;
    private String userId;
    private String userName;

    public static LoginResponse from(AuthDto authDto) {
        return LoginResponse.builder()
                .token(authDto.getToken())
                .userId(authDto.getUniqueId())
                .userName(authDto.getName())
                .build();
    }
}
