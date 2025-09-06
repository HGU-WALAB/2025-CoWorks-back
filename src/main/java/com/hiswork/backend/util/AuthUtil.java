package com.hiswork.backend.util;

import com.hiswork.backend.domain.User;
import com.hiswork.backend.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthUtil {
    
    private final UserRepository userRepository;
    
    @Value("${jwt.secret_key}")
    private String SECRET_KEY;
    
    public User getCurrentUser(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        if (token == null) {
            throw new RuntimeException("인증 토큰이 없습니다.");
        }
        
        Key key = JwtUtil.getSigningKey(SECRET_KEY);
        
        if (!JwtUtil.validateToken(token, key)) {
            throw new RuntimeException("유효하지 않은 토큰입니다.");
        }
        
        String uniqueId = JwtUtil.getUniqueIdFromToken(token, key);
        return userRepository.findById(uniqueId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
    }
    
    private String extractTokenFromRequest(HttpServletRequest request) {
        // 1. 쿠키에서 토큰 추출 시도
        String token = extractTokenFromCookie(request);
        if (token != null) {
            log.debug("쿠키에서 토큰 추출 성공");
            return token;
        }
        
        // 2. Authorization 헤더에서 토큰 추출 시도 (Bearer 방식)
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            log.debug("Authorization 헤더에서 토큰 추출 성공");
            return bearerToken.substring(7);
        }
        
        log.debug("토큰을 찾을 수 없음");
        return null;
    }
    
    private String extractTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        
        for (Cookie cookie : request.getCookies()) {
            if ("accessToken_hiswork".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
} 