package com.hiswork.backend.controller;

import com.hiswork.backend.dto.AuthDto;
import com.hiswork.backend.dto.LoginRequest;
import com.hiswork.backend.dto.LoginResponse;
import com.hiswork.backend.dto.MeResponse;
import com.hiswork.backend.dto.SignUpResponse;
import com.hiswork.backend.dto.SignupRequest;
import com.hiswork.backend.service.AuthService;
import com.hiswork.backend.service.HisnetLoginService;
import com.hiswork.backend.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final AuthService authService;
    private final AuthUtil authUtil;
    private final HisnetLoginService hisnetLoginService;

    //test
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {
        try {
            log.info("회원가입 요청 : {}", request.getEmail());
            SignUpResponse signUpResponse = authService.signup(request);
            return ResponseEntity.ok(signUpResponse);
        } catch (Exception e) {
            log.error("Signup error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // 로그인 테스트
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            log.info("로그인 요청: {}", request.getEmail());
            SignUpResponse response = authService.login(request);
            log.info("로그인 성공: {}", request.getEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("로그인 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // 사용자 정보 조회
    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        try {
            var user = authUtil.getCurrentUser(request);
            return ResponseEntity.ok(MeResponse.from(user));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "인증이 필요합니다"));
        }
    }


    // 히즈넷 로그인
    @PostMapping("/hisnet-login")
    public ResponseEntity<LoginResponse> hisNetlogin(@RequestBody LoginRequest request) {
        // 1. 히즈넷 로그인 API 호출 -> 사용자 정보 가져옴
        AuthDto authDto = hisnetLoginService.callHisnetLoginApi(AuthDto.from(request));

        // 2. 사용자 정보로 로그인 처리 -> JWT 토큰 생성
        LoginResponse loginResponse = LoginResponse.from(authService.login(authDto));

        // 3. 응답 body에 토큰 포함
        return ResponseEntity.ok(loginResponse);
    }

    @GetMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        return ResponseEntity.ok(Map.of("message", "로그아웃 성공. 프론트엔드에서 localStorage의 토큰을 삭제해주세요."));
    }
} 