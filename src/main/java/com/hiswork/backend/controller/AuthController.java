package com.hiswork.backend.controller;

import com.hiswork.backend.dto.*;
import com.hiswork.backend.service.AuthService;
import com.hiswork.backend.service.HisnetLoginService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hiswork/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final HisnetLoginService hisnetLoginService;

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request){
        // 1. 히즈넷 로그인 API 호출 -> 사용자 정보 가져옴
        AuthDto authDto = hisnetLoginService.callHisnetLoginApi(AuthDto.from(request));

        // 2. 사용자 정보로 로그인 처리 -> JWT 토큰 생성
        LoginResponse loginResponse = LoginResponse.from(authService.login(authDto));

        // 3. 토큰 응답
        String accessToken_hiswork = loginResponse.getToken();
        String refreshToken_hiswork = authService.createRefreshToken(
                loginResponse.getUserId(),
                loginResponse.getUserName()
        );

        // 4. 토큰을 쿠키에 담아 응답 헤더에 추가
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, "accessToken_hiswork=" + accessToken_hiswork + "; HttpOnly; Secure; Path=/; Max-Age=7200; SameSite=Strict;");
        headers.add(HttpHeaders.SET_COOKIE, "refreshToken_hiswork=" + refreshToken_hiswork + "; HttpOnly; Secure; Path=/; Max-Age=604800; SameSite=Strict;");

        return ResponseEntity.ok()
                .headers(headers)
                .body(loginResponse);
    }

    // 로그아웃
    @GetMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        Cookie accessCookie = new Cookie("accessToken_hiswork", "");
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(false);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(0); // 쿠키 삭제

        Cookie refreshCookie = new Cookie("refreshToken_hiswork", "");
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0); // 쿠키 삭제

        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);
        return ResponseEntity.ok().build();
    }
} 