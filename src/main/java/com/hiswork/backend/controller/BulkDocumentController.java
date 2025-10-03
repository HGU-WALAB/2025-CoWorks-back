package com.hiswork.backend.controller;

import com.hiswork.backend.annotation.RequireFolderAccess;
import com.hiswork.backend.domain.BulkStaging;
import com.hiswork.backend.domain.User;
import com.hiswork.backend.dto.*;
import com.hiswork.backend.repository.BulkStagingRepository;
import com.hiswork.backend.service.BulkDocumentService;
import com.hiswork.backend.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/documents/bulk")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BulkDocumentController {
    
    private final BulkDocumentService bulkDocumentService;
    private final BulkStagingRepository bulkStagingRepository;
    private final AuthUtil authUtil;
    
    // 엑셀 파일 업로드 및 데이터 임시저장
    @PostMapping("/preview")
    @RequireFolderAccess
    public ResponseEntity<?> createPreview(
            @RequestParam("file") MultipartFile file,
            @RequestParam("templateId") Long templateId,
            HttpServletRequest httpRequest) {
        
        try {
            log.info("대량 업로드 요청 - 템플릿 ID: {}, 파일: {}", templateId, file.getOriginalFilename());
            
            // 1. 사용자 인증
            User currentUser = getCurrentUser(httpRequest);
            
            // 2. 파일 유효성 검사
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "파일이 비어있습니다"));
            }
            
            String filename = file.getOriginalFilename();
            if (filename == null || (!filename.toLowerCase().endsWith(".xlsx") && 
                                   !filename.toLowerCase().endsWith(".xls") && 
                                   !filename.toLowerCase().endsWith(".csv"))) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "지원하지 않는 파일 형식입니다. Excel(.xlsx, .xls) 또는 CSV 파일만 업로드 가능합니다"));
            }
            
            // 3. 파일 업로드 및 임시 저장
            BulkPreviewResponse response = bulkDocumentService.createPreview(file, templateId, currentUser);
            
            log.info("파일 업로드 완료 - 스테이징 ID: {}, 유효 행: {}/{}", 
                    response.getStagingId(), response.getValidRows(), response.getTotalRows());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("파일 업로드 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    // 엑셀 파일 문서 생성 확정 (preview -> commit)
    @PostMapping("/commit")
    @RequireFolderAccess
    public ResponseEntity<?> commitBulkCreation(
            @Valid @RequestBody BulkCommitRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            log.info("대량 문서 생성 확정 요청 - 스테이징 ID: {}, 중복 처리: {}", 
                    request.getStagingId(), request.getOnDuplicate());
            
            // 1. 사용자 인증
            User currentUser = getCurrentUser(httpRequest);
            
            // 2. 대량 문서 생성
            BulkCommitResponse response = bulkDocumentService.commitBulkCreation(request, currentUser);
            
            log.info("대량 문서 생성 완료 - 생성: {}, 건너뜀: {}, 실패: {}", 
                    response.getCreated(), response.getSkipped(), response.getFailed());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("대량 문서 생성 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
     // 스테이징 상태 확인
    @GetMapping("/staging/{stagingId}/status")
    @RequireFolderAccess
    public ResponseEntity<?> getStagingStatus(
            @PathVariable String stagingId,
            HttpServletRequest httpRequest) {
        
        try {
            log.info("스테이징 상태 확인 요청 - 스테이징 ID: {}", stagingId);
            
            // 1. 사용자 인증
            User currentUser = getCurrentUser(httpRequest);
            log.info("요청자 정보 - ID: {}, 이메일: {}", currentUser.getId(), currentUser.getEmail());
            
            // 2. 스테이징 조회 (권한 확인 없이)
            Optional<BulkStaging> stagingOpt = bulkStagingRepository.findById(stagingId);
            
            if (stagingOpt.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "stagingId", stagingId,
                    "exists", false,
                    "message", "스테이징이 존재하지 않습니다"
                ));
            }
            
            BulkStaging staging = stagingOpt.get();
            
            return ResponseEntity.ok(Map.of(
                "stagingId", stagingId,
                "exists", true,
                "status", staging.getStatus().name(),
                "creatorId", staging.getCreator().getId(),
                "creatorEmail", staging.getCreator().getEmail(),
                "requestorId", currentUser.getId(),
                "requestorEmail", currentUser.getEmail(),
                "hasPermission", staging.getCreator().getId().equals(currentUser.getId()),
                "canCommit", staging.canCommit(),
                "canCancel", staging.canCancel()
            ));
            
        } catch (Exception e) {
            log.error("스테이징 상태 확인 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }


    // 특정 템플릿으로 생성한 스테이징 목록 조회
    @GetMapping("/getStaging")
    public ResponseEntity<?> getStagingsByTemplate(
            @RequestParam Long templateId,
            HttpServletRequest httpRequest
    ) {
        User currentUser = getCurrentUser(httpRequest);
        List<BulkStaging> stagings = bulkStagingRepository
                .findByTemplateIdAndCreatorOrderByCreatedAtDesc(templateId, currentUser.getId());

        // stagingId만 추출하여 반환
        List<String> stagingIds = stagings.stream()
                .map(BulkStaging::getStagingId)
                .toList();

        return ResponseEntity.ok(stagingIds);
    }


    // staging에 있는 아이템 조회
    @GetMapping("/staging/{stagingId}/items")
    @RequireFolderAccess
    public ResponseEntity<?> getStagingItems(
            @PathVariable String stagingId,
            HttpServletRequest httpRequest) {
        
        try {
            log.info("스테이징 아이템 조회 요청 - 스테이징 ID: {}", stagingId);
            
            // 1. 사용자 인증
            User currentUser = getCurrentUser(httpRequest);
            
            // 2. 스테이징 아이템 조회
            BulkStagingItemsResponse response = bulkDocumentService.getStagingItems(stagingId, currentUser);
            
            log.info("스테이징 아이템 조회 완료 - 스테이징 ID: {}, 아이템 수: {}", 
                    stagingId, response.getItems().size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("스테이징 아이템 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    // 파일 업로드 취소 (preview -> cancel)
    @PostMapping("/cancel")
    @RequireFolderAccess
    public ResponseEntity<?> cancelBulkUpload(
            @Valid @RequestBody BulkCancelRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            log.info("대량 업로드 취소 요청 - 스테이징 ID: {}", request.getStagingId());
            
            // 1. 사용자 인증
            User currentUser = getCurrentUser(httpRequest);
            // 2. 업로드 취소
            BulkCancelResponse response = bulkDocumentService.cancelBulkUpload(request, currentUser);
            log.info("대량 업로드 취소 완료 - 스테이징 ID: {}", request.getStagingId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("대량 업로드 취소 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // 현재 사용자 정보 추출
    private User getCurrentUser(HttpServletRequest request) {
        try {
            log.debug("JWT 토큰에서 사용자 정보 추출");
            
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new RuntimeException("인증이 필요합니다");
            }
            
            User user = authUtil.getCurrentUser(request);
            log.debug("인증된 사용자: {} ({})", user.getName(), user.getEmail());
            
            return user;
            
        } catch (Exception e) {
            log.error("사용자 인증 실패: {}", e.getMessage());
            throw new RuntimeException("인증이 필요합니다. 로그인 후 다시 시도해주세요.");
        }
    }
}
