package com.hiswork.backend.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Date;

@Component
@Slf4j
public class JwtUtil {
    private static final long EXPIRE_TIME_MS = 1000 * 60 * 60 * 24 * 7;  // 7 days - 개발 환경 편의성을 위해 임시로 수정
    private static final long REFRESH_EXPIRE_TIME_MS = 1000 * 60 * 60 * 24 * 7;  // 7 days


    @Value("${jwt.secret_key}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}") // 24시간 (밀리초)
    private int jwtExpirationMs;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateToken(String email) {
        Date expiryDate = new Date(System.currentTimeMillis() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public String generateToken(com.hiswork.backend.domain.User user) {
        return generateToken(user.getEmail());
    }

    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    // SecretKey를 이용해 키 생성
    public static Key getSigningKey(String secretKey) {
        byte[] keyBytes = secretKey.getBytes();
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, "HmacSHA256");
    }

//    // JWT access token 발급
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
                .signWith(secretKey, SignatureAlgorithm.HS256)
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
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // JWT 토큰 검증
    public static boolean validateToken(String token, Key secretKey) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    // JWT 토큰에서 Claims 추출
    public static Claims getClaims(String token, Key secretKey) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // JWT 토큰에서 uniqueId 추출
    public static String getUniqueIdFromToken(String token, Key secretKey) {
        Claims claims = getClaims(token, secretKey);
        return claims.get("uniqueId", String.class);
    }

    // JWT 토큰에서 name 추출
    public static String getNameFromToken(String token, Key secretKey) {
        Claims claims = getClaims(token, secretKey);
        return claims.get("name", String.class);
    }

    // JWT 토큰에서 department 추출
    public static String getDepartmentFromToken(String token, Key secretKey) {
        Claims claims = getClaims(token, secretKey);
        return claims.get("department", String.class);
    }
} 