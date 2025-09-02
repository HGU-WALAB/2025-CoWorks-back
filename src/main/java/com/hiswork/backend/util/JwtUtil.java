package com.hiswork.backend.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Date;

@Component
@Slf4j
public class JwtUtil {
    private static final long EXPIRE_TIME_MS = 1000 * 60 * 60 * 24 * 7;  // 7 days - 개발 환경 편의성을 위해 임시로 수정
    private static final long REFRESH_EXPIRE_TIME_MS = 1000 * 60 * 60 * 24 * 7;  // 7 days

    // SecretKey를 이용해 키 생성
    public static Key getSigningKey(String secretKey) {
        byte[] keyBytes = secretKey.getBytes();
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, "HmacSHA256");
    }

    // JWT access token 발급
    public static String createToken(String uniqueId, String name, String department, Key secretKey) {
        Claims claims = Jwts.claims();
        claims.put("uniqueId", uniqueId);
        claims.put("name", name);
        claims.put("department", department);
        log.info("Creating JWT access token for user: {}", name);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE_TIME_MS))
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }

    // JWT refresh token 발급
    public static String createRefreshToken(String uniqueId, String name, Key secretKey) {
        Claims claims = Jwts.claims();
        claims.put("uniqueId", uniqueId);
        claims.put("name", name);
        log.info("Creating JWT refresh token for user: {}", name);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_EXPIRE_TIME_MS))
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }
} 