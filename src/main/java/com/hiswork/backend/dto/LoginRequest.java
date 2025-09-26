package com.hiswork.backend.dto;
import jakarta.validation.constraints.Email;
import lombok.Data;
import lombok.Getter;

@Getter
@Data
public class LoginRequest {
    private String hisnetToken; // 히즈넷에서 받은 토큰

    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    private String password;
}