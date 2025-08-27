package com.hiswork.backend.service;

import com.hiswork.backend.domain.User;
import com.hiswork.backend.dto.AuthResponse;
import com.hiswork.backend.dto.LoginRequest;
import com.hiswork.backend.dto.SignupRequest;
import com.hiswork.backend.repository.UserRepository;
import com.hiswork.backend.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    
    public AuthResponse signup(SignupRequest request) {
        // 이메일 중복 확인
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("이미 존재하는 이메일입니다");
        }
        
        // Position 검증 및 변환
        if (!request.isValidPosition()) {
            throw new RuntimeException("유효하지 않은 직분입니다. 허용된 값: 교직원, 교수, 학생, 연구원, 행정직원, 기타");
        }
        
        User.Position position = User.Position.valueOf(request.getPosition());
        
        // 사용자 생성
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .position(position)
                .role(User.Role.USER)
                .build();
        
        user = userRepository.save(user);
        
        // JWT 토큰 생성
        String token = jwtUtil.generateToken(user);
        
        return AuthResponse.from(user, token);
    }
    
    public AuthResponse login(LoginRequest request) {
        // 사용자 찾기
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 이메일입니다"));
        
        // 비밀번호 확인
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다");
        }
        
        // JWT 토큰 생성
        String token = jwtUtil.generateToken(user);
        
        return AuthResponse.from(user, token);
    }
} 