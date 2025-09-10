package com.hiswork.backend.service;

import com.hiswork.backend.domain.User;
import com.hiswork.backend.dto.AuthDto;
import com.hiswork.backend.repository.UserRepository;
import com.hiswork.backend.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Key;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;

    @Value("${jwt.secret_key}")
    private String SECRET_KEY;

    public User getLoginUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
    }

    // 로그인
    @Transactional
    public AuthDto login(AuthDto authDto) {
        // 사용자 찾기
        Optional<User> user = userRepository.findByUniqueId(authDto.getUniqueId());
        User loggedInUser = user.orElseGet(() -> User.from(authDto));
        userRepository.save(loggedInUser);

        Key key = JwtUtil.getSigningKey(SECRET_KEY);

        String accessToken_hiswork = JwtUtil.createToken(
                loggedInUser.getUniqueId(),
                loggedInUser.getName(),
                loggedInUser.getDepartment(),
                key
        );

        log.info("✅ Generated AccessToken: {}", accessToken_hiswork);

        // JWT 토큰과 사용자 정보 반환
        return AuthDto.builder()
                .token(accessToken_hiswork) // JWT 토큰
                .uniqueId(loggedInUser.getUniqueId())
                .name(loggedInUser.getName())
                .email(loggedInUser.getEmail()) // 편집자/검토자 찾을 때 필요할 것 같아서 추가
                .department(loggedInUser.getDepartment())
                .build();
    }

    // AccessToken 생성
    public String createAccessToken(String uniqueId, String name, String department) {
        Key key = JwtUtil.getSigningKey(SECRET_KEY);
        return JwtUtil.createToken(uniqueId, name, department, key);
    }

    // RefreshToken 생성
    public String createRefreshToken(String uniqueId, String name, String department) {
        Key key = JwtUtil.getSigningKey(SECRET_KEY);
        return JwtUtil.createRefreshToken(uniqueId, name, key);
    }

} 