package com.hiswork.backend.controller;

import com.hiswork.backend.domain.User;
import com.hiswork.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class UserController {
    
    private final UserRepository userRepository;
    
    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> searchUsers(
            @RequestParam(required = false) String query) {
        
        try {
            List<User> users;
            
            if (query == null || query.trim().isEmpty()) {
                // 검색어가 없으면 모든 사용자 반환 (최대 50명)
                users = userRepository.findAll().stream()
                        .limit(50)
                        .collect(Collectors.toList());
            } else {
                // 이메일 또는 이름으로 검색
                users = userRepository.findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase(
                        query.trim(), query.trim()
                ).stream()
                        .limit(20)
                        .collect(Collectors.toList());
            }
            
            // 민감한 정보 제외하고 반환
            List<Map<String, Object>> result = users.stream()
                    .map(user -> {
                        Map<String, Object> userMap = Map.of(
                                "id", user.getId(),
                                "email", user.getEmail(),
                                "name", user.getName(),
                                "position", user.getPosition() != null ? user.getPosition().name() : ""
                        );
                        return userMap;
                    })
                    .collect(Collectors.toList());
            
            log.info("사용자 검색 결과: query='{}', count={}", query, result.size());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("사용자 검색 중 오류 발생: query='{}'", query, e);
            return ResponseEntity.ok(List.of()); // 빈 리스트 반환
        }
    }
} 