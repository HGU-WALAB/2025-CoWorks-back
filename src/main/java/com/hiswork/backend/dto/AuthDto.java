package com.hiswork.backend.dto;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
// 사용자 정보 DTO
public class AuthDto {
    private String hisnetToken; // 히즈넷에서 받은 토큰
    private String token; // jwt
    private String uniqueId;
    private String name;
    private String email;
    private String department;
    private String major1;
    private String major2;
    private Integer grade;
    private Integer semester;

    // LoginRequest -> AuthDto로 변환
    public static AuthDto from(LoginRequest request) {
        return AuthDto.builder().
                hisnetToken(request.getHisnetToken())
                .build();
    }
}