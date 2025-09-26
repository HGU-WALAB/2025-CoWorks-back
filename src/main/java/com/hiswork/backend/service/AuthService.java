package com.hiswork.backend.service;

import com.hiswork.backend.domain.User;
import com.hiswork.backend.domain.DocumentRole;
import com.hiswork.backend.dto.*;
import com.hiswork.backend.repository.UserRepository;
import com.hiswork.backend.repository.DocumentRoleRepository;
import com.hiswork.backend.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

 import java.security.Key;
 import java.util.Optional;
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final DocumentRoleRepository documentRoleRepository;

    @Value("${jwt.secret_key}")
    private String SECRET_KEY;

    public User getLoginUser(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
    }

    // test용 회원가입
    @Transactional
    public SignUpResponse signup(SignupRequest request){
        log.info("회원가입 시작 - 이메일: {}, 이름: {}, 직분: {}", 
                request.getEmail(), request.getName(), request.getPosition());

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("이미 존재하는 이메일: {}", request.getEmail());
            throw new RuntimeException("이미 존재하는 이메일입니다");
        }

        // SignupRequest의 position 검증
        if (!request.isValidPosition()) {
            log.warn("유효하지 않은 직분: {}", request.getPosition());
            throw new RuntimeException("유효하지 않은 직분입니다");
        }

        User user = User.builder()
                .id(java.util.UUID.randomUUID().toString()) // UUID를 String으로 생성
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .position(com.hiswork.backend.domain.Position.valueOf(request.getPosition())) // SignupRequest에서 받은 position 사용
                .role(com.hiswork.backend.domain.Role.USER) // 기본값으로 USER 설정
                .build();

        log.info("사용자 객체 생성 완료 - ID: {}, 이메일: {}", user.getId(), user.getEmail());

        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // 동시 요청으로 인한 이메일 유니크 충돌 시 재조회 후 그대로 진행
            log.warn("이메일 유니크 충돌 감지, 재조회 수행: {}", user.getEmail());
            user = userRepository.findByEmail(user.getEmail())
                    .orElseThrow(() -> e);
        }
        log.info("사용자 데이터베이스 저장 완료 - ID: {}, 이메일: {}", user.getId(), user.getEmail());

        // 임시 할당된 문서들을 실제 사용자에게 연결
        int linkedDocuments = linkPendingDocuments(user);
        if (linkedDocuments > 0) {
            log.info("임시 할당된 문서 {}개를 사용자에게 연결했습니다: {}", linkedDocuments, user.getEmail());
        }

        // JWT 토큰 생성
        String token = jwtUtil.generateToken(user);
        log.info("JWT 토큰 생성 완료 - 이메일: {}", user.getEmail());

        SignUpResponse response = SignUpResponse.from(user, token);
        log.info("회원가입 완료 - 이메일: {}, 토큰 길이: {}, 연결된 문서: {}개", user.getEmail(), token.length(), linkedDocuments);
        
        return response;
    }

    // 테스트 로그인
    public SignUpResponse login(LoginRequest request){
        // 사용자 찾기
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 이메일입니다"));

        // 비밀번호 확인 (해시된 비밀번호와 평문 비밀번호 비교)
        if(!passwordEncoder.matches(request.getPassword(), user.getPassword())){
            throw new RuntimeException("비밀번호가 일치하지 않습니다");
        }

        // JWT 토큰 생성
        String token = jwtUtil.generateToken(user);

        return SignUpResponse.from(user, token);
    }

    /**
     * 회원가입 시 임시 할당된 문서들을 실제 사용자에게 연결
     */
    private int linkPendingDocuments(User newUser) {
        List<DocumentRole> pendingRoles = new ArrayList<>();
        
        // 이메일로 임시 할당된 문서들 검색
        pendingRoles.addAll(documentRoleRepository.findByPendingEmail(newUser.getEmail()));
        
        // ID(학번)로 임시 할당된 문서들 검색 (assignedUserId에 저장됨)
        if (newUser.getId() != null) {
            pendingRoles.addAll(documentRoleRepository.findByPendingUserId(newUser.getId()));
        }
        
        int linkedCount = 0;
        for (DocumentRole role : pendingRoles) {
            // 임시 할당을 실제 사용자로 전환
            role.setAssignedUserId(newUser.getId());
            role.setPendingEmail(null);
            role.setPendingName(null);
            
            documentRoleRepository.save(role);
            
            linkedCount++;
            log.info("임시 할당 문서를 실제 사용자에게 연결: {} -> {}", role.getDocument().getTitle(), newUser.getEmail());
        }
        
        return linkedCount;
    }

    // 히즈넷 로그인
    @Transactional
    public AuthDto login(AuthDto authDto) {
        // 사용자 찾기
        Optional<User> user = userRepository.findById(authDto.getUniqueId());
        User loggedInUser = user.orElseGet(() -> User.from(authDto));
        userRepository.save(loggedInUser);

        Key key = JwtUtil.getSigningKey(SECRET_KEY);

        String accessToken_hiswork = JwtUtil.createToken(
                loggedInUser.getId(),
                loggedInUser.getName(),
                loggedInUser.getDepartment(),
                key
        );

        log.info("✅ Generated AccessToken: {}", accessToken_hiswork);

        // JWT 토큰과 사용자 정보 반환
        return AuthDto.builder()
                .token(accessToken_hiswork) // JWT 토큰
                .uniqueId(loggedInUser.getId())
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