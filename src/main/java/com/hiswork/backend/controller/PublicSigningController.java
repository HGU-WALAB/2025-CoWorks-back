package com.hiswork.backend.controller;

import com.hiswork.backend.domain.Document;
import com.hiswork.backend.domain.SigningToken;
import com.hiswork.backend.domain.User;
import com.hiswork.backend.dto.DocumentResponse;
import com.hiswork.backend.exception.InvalidTokenException;
import com.hiswork.backend.repository.UserRepository;
import com.hiswork.backend.service.DocumentService;
import com.hiswork.backend.service.SigningTokenService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/public/sign")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PublicSigningController {
    
    private final SigningTokenService tokenService;
    private final DocumentService documentService;
    private final UserRepository userRepository;
    
    /**
     * 토큰 기반 문서 조회 (익명 사용자)
     */
    @GetMapping("/{documentId}")
    public ResponseEntity<?> getDocumentForSigning(
        @PathVariable Long documentId,
        @RequestParam String token
    ) {
        log.info("공개 서명 - 문서 조회 요청 - 문서 ID: {}, 토큰: {}", documentId, token);
        
        try {
            // 문서 조회 (토큰 검증 전에 먼저 문서 상태 확인)
            Document document = documentService.getDocumentById(documentId)
                .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다"));
            
            // 문서가 완료된 경우, 토큰이 사용되었더라도 조회 허용
            SigningToken signingToken;
            if (document.getStatus().equals(Document.DocumentStatus.COMPLETED)) {
                // 완료된 문서의 경우 토큰 존재 여부만 확인 (유효성 검증 안 함)
                signingToken = tokenService.findTokenByValue(token)
                    .orElseThrow(() -> new InvalidTokenException("유효하지 않은 토큰입니다"));
                
                // 문서 ID가 일치하는지만 확인
                if (!signingToken.getDocumentId().equals(documentId)) {
                    throw new InvalidTokenException("문서 ID가 일치하지 않습니다");
                }
            } else {
                // 서명 진행 중인 문서는 토큰 유효성 검증
                signingToken = tokenService.validateToken(token, documentId);
                
                // 문서 상태 확인
                if (!document.getStatus().equals(Document.DocumentStatus.SIGNING)) {
                    log.warn("서명 불가능한 문서 상태 - 문서 ID: {}, 상태: {}", documentId, document.getStatus());
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "서명 가능한 상태가 아닙니다"));
                }
            }
            
            // 서명자 권한 확인
            boolean hasSignerRole = document.getDocumentRoles().stream()
                .anyMatch(role -> {
                    if (!role.getTaskRole().equals(com.hiswork.backend.domain.DocumentRole.TaskRole.SIGNER)) {
                        return false;
                    }
                    // assignedUserId로 사용자 조회
                    User user = userRepository.findById(role.getAssignedUserId()).orElse(null);
                    return user != null && user.getEmail().equals(signingToken.getSignerEmail());
                });
            
            if (!hasSignerRole) {
                log.warn("서명 권한 없음 - 문서 ID: {}, 서명자: {}", documentId, signingToken.getSignerEmail());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "이 문서의 서명 권한이 없습니다"));
            }
            
            // 문서 정보 반환
            DocumentResponse documentResponse = documentService.getDocumentResponse(documentId);
            
            log.info("공개 서명 - 문서 조회 성공 - 문서 ID: {}, 서명자: {}, 문서 상태: {}", 
                documentId, signingToken.getSignerEmail(), document.getStatus());
            
            return ResponseEntity.ok(Map.of(
                "document", documentResponse,
                "signerEmail", signingToken.getSignerEmail()
            ));
            
        } catch (InvalidTokenException e) {
            log.warn("토큰 검증 실패 - 문서 ID: {}, 오류: {}", documentId, e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("공개 서명 - 문서 조회 실패 - 문서 ID: {}", documentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "문서 조회 중 오류가 발생했습니다"));
        }
    }
    
    /**
     * 토큰 기반 서명 처리 (익명 사용자)
     */
    @PostMapping("/{documentId}")
    public ResponseEntity<?> signDocument(
        @PathVariable Long documentId,
        @RequestParam String token,
        @RequestBody Map<String, String> request
    ) {
        log.info("공개 서명 - 서명 처리 요청 - 문서 ID: {}", documentId);
        
        try {
            // 토큰 검증
            SigningToken signingToken = tokenService.validateToken(token, documentId);
            String signature = request.get("signature");
            
            if (signature == null || signature.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "서명 데이터가 필요합니다"));
            }
            
            // 서명 처리 (이메일로 사용자 찾아서 처리)
            documentService.approveDocumentByEmail(documentId, signingToken.getSignerEmail(), signature);
            
            // 토큰 사용 완료 처리
            tokenService.markTokenAsUsed(token);
            
            log.info("공개 서명 - 서명 처리 성공 - 문서 ID: {}, 서명자: {}", 
                documentId, signingToken.getSignerEmail());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "서명이 완료되었습니다"
            ));
            
        } catch (InvalidTokenException e) {
            log.warn("토큰 검증 실패 - 문서 ID: {}, 오류: {}", documentId, e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("공개 서명 - 서명 처리 실패 - 문서 ID: {}", documentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "서명 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 토큰 기반 문서 반려 (익명 사용자)
     */
    @PostMapping("/{documentId}/reject")
    public ResponseEntity<?> rejectDocument(
        @PathVariable Long documentId,
        @RequestParam String token,
        @RequestBody Map<String, String> request
    ) {
        log.info("공개 서명 - 반려 처리 요청 - 문서 ID: {}", documentId);
        
        try {
            // 토큰 검증
            SigningToken signingToken = tokenService.validateToken(token, documentId);
            String reason = request.get("reason");
            
            if (reason == null || reason.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "반려 사유가 필요합니다"));
            }
            
            // 반려 처리 (이메일로 사용자 찾아서 처리)
            documentService.rejectDocumentByEmail(documentId, signingToken.getSignerEmail(), reason);
            
            // 토큰 사용 완료 처리
            tokenService.markTokenAsUsed(token);
            
            log.info("공개 서명 - 반려 처리 성공 - 문서 ID: {}, 서명자: {}", 
                documentId, signingToken.getSignerEmail());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "문서가 반려되었습니다"
            ));
            
        } catch (InvalidTokenException e) {
            log.warn("토큰 검증 실패 - 문서 ID: {}, 오류: {}", documentId, e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("공개 서명 - 반려 처리 실패 - 문서 ID: {}", documentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "반려 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}
