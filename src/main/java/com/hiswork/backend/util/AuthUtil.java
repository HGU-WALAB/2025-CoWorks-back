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
import io.jsonwebtoken.Claims;

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
        if (uniqueId != null && !uniqueId.isBlank()) {
            return userRepository.findById(uniqueId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: uniqueId=" + uniqueId));
        }

        // 2) 일반 토큰: subject(email)
        Claims claims = JwtUtil.getClaims(token, key);
        String email = claims.getSubject();
        log.info("JWT 토큰에서 추출된 이메일: {}", email);

        if (email == null || email.isBlank()) {
            throw new RuntimeException("JWT 토큰에서 이메일을 찾을 수 없습니다.");
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("이메일로 사용자를 찾을 수 없습니다: " + email));
    }
    
    private String extractTokenFromRequest(HttpServletRequest request) {
        // 1. Authorization 헤더에서 토큰 추출 시도 (localStorage 방식)
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            log.debug("Authorization 헤더에서 토큰 추출 성공");
            return bearerToken.substring(7);
        }
        
        // 2. Request attribute에서 토큰 추출 시도 (SSE용)
        String attributeAuth = (String) request.getAttribute("Authorization");
        if (attributeAuth != null && attributeAuth.startsWith("Bearer ")) {
            log.debug("Request attribute에서 토큰 추출 성공");
            return attributeAuth.substring(7);
        }
        
        // 3. 쿼리 파라미터에서 토큰 추출 시도 (SSE용 추가)
        String queryToken = request.getParameter("token");
        if (queryToken != null && !queryToken.isEmpty()) {
            log.debug("쿼리 파라미터에서 토큰 추출 성공");
            return queryToken;
        }
        
        log.debug("토큰을 찾을 수 없음");
        return null;
    }
} 