package com.hiswork.backend.controller;

import com.hiswork.backend.domain.Document;
import com.hiswork.backend.domain.User;
import com.hiswork.backend.domain.Position;
import com.hiswork.backend.domain.Role;
import com.hiswork.backend.dto.DocumentCreateRequest;
import com.hiswork.backend.dto.DocumentHistoryResponse;
import com.hiswork.backend.dto.DocumentResponse;
import com.hiswork.backend.dto.DocumentUpdateRequest;
import com.hiswork.backend.repository.UserRepository;
import com.hiswork.backend.service.DocumentHistoryService;
import com.hiswork.backend.service.DocumentService;
import com.hiswork.backend.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Optional;
import com.hiswork.backend.service.PdfService;

@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DocumentController {
    
    private final DocumentService documentService;
    private final DocumentHistoryService documentHistoryService;
    private final UserRepository userRepository;
    private final AuthUtil authUtil;
    private final PdfService pdfService;
    
    @PostMapping
    public ResponseEntity<?> createDocument(
            @Valid @RequestBody DocumentCreateRequest request,
            HttpServletRequest httpRequest) {
        
        log.info("Document creation request: {}", request);
        
        try {
            User creator = getCurrentUser(httpRequest);
            log.info("Creator user: {}", creator.getId());
            
            Document document = documentService.createDocument(
                    request.getTemplateId(), 
                    creator, 
                    request.getEditorEmail(),
                    request.getTitle()
            );
            
            log.info("Document created successfully with ID: {}", document.getId());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(DocumentResponse.from(document));
        } catch (Exception e) {
            log.error("Error creating document", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getAllDocuments(HttpServletRequest httpRequest) {
        try {
            User currentUser = getCurrentUser(httpRequest);
            List<Document> documents = documentService.getDocumentsByUser(currentUser);
            List<DocumentResponse> responses = documents.stream()
                    .map(DocumentResponse::from)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error getting all documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocument(@PathVariable Long id) {
        try {
            Optional<Document> documentOpt = documentService.getDocumentById(id);
            if (documentOpt.isPresent()) {
                return ResponseEntity.ok(DocumentResponse.from(documentOpt.get()));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error getting document {}", id, e);
            return ResponseEntity.notFound().build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateDocument(
            @PathVariable Long id, 
            @Valid @RequestBody DocumentUpdateRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            log.info("Updating document {} with data: {}", id, request.getData());
            
            User user = getCurrentUser(httpRequest);
            Document document = documentService.updateDocumentData(id, request, user);
            
            log.info("Document updated successfully: {}", id);
            return ResponseEntity.ok(DocumentResponse.from(document));
        } catch (Exception e) {
            log.error("Error updating document {}", id, e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/{id}/assign-editor")
    public ResponseEntity<?> assignEditor(
            @PathVariable Long id, 
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        
        try {
            String editorEmail = request.get("editorEmail");
            User user = getCurrentUser(httpRequest);
            
            Document document = documentService.assignEditor(id, editorEmail, user);
            log.info("Editor assigned successfully to document {}", id);
            return ResponseEntity.ok(DocumentResponse.from(document));
        } catch (Exception e) {
            log.error("Error assigning editor to document {}", id, e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/{id}/assign-reviewer")
    public ResponseEntity<?> assignReviewer(
            @PathVariable Long id, 
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        
        try {
            String reviewerEmail = request.get("reviewerEmail");
            User user = getCurrentUser(httpRequest);
            
            log.info("검토자 할당 요청 - 문서 ID: {}, 검토자: {}, 요청자: {}", 
//                    id, reviewerEmail, user.getId());
                    id, reviewerEmail, user.getEmail());
            
            Document document = documentService.assignReviewer(id, reviewerEmail, user);
            log.info("Reviewer assigned successfully to document {}", id);
            return ResponseEntity.ok(DocumentResponse.from(document));
        } catch (Exception e) {
            log.error("Error assigning reviewer to document {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/{id}/submit-for-review")
    public ResponseEntity<?> submitForReview(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        
        try {
            User user = getCurrentUser(httpRequest);
            Document document = documentService.submitForReview(id, user);
            log.info("Document submitted for review successfully: {}", id);
            return ResponseEntity.ok(DocumentResponse.from(document));
        } catch (Exception e) {
            log.error("Error submitting document for review {}", id, e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/{id}/download-pdf")
    public ResponseEntity<?> downloadPdf(@PathVariable Long id, HttpServletRequest httpRequest) {
        try {
            User user = getCurrentUser(httpRequest);
            
            // 문서 조회
            Document document = documentService.getDocumentById(id)
                    .orElseThrow(() -> new RuntimeException("Document not found"));
            
            // PDF 기반 템플릿인지 확인 (pdfFilePath가 있는지로 판단)
            if (document.getTemplate().getPdfFilePath() == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "PDF 다운로드는 PDF 기반 템플릿만 지원됩니다."));
            }
            
            // PDF 생성
            String completedPdfPath = pdfService.generateCompletedPdf(
                document.getTemplate().getPdfFilePath(),
                null, // coordinateFields는 더 이상 사용하지 않음
                document.getData(),
                document.getTemplate().getName()
            );
            
            log.info("PDF 다운로드 요청 - 문서 ID: {}, 상태: {}", id, document.getStatus());
            log.info("템플릿 파일 경로: {}", document.getTemplate().getPdfFilePath());
            log.info("문서 데이터: {}", document.getData());
            
            // 생성된 PDF 파일을 바이트 배열로 읽기
            byte[] pdfBytes = Files.readAllBytes(Paths.get(completedPdfPath));
            
            // 파일명 설정 (한글 파일명 지원)
            String filename = document.getTemplate().getName() + "_완성본.pdf";
            String encodedFilename = java.net.URLEncoder.encode(filename, "UTF-8")
                .replaceAll("\\+", "%20");
            
            // PDF 파일 반환
            return ResponseEntity.ok()
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFilename)
                    .body(pdfBytes);
            
        } catch (Exception e) {
            log.error("PDF 다운로드 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/{documentId}/start-editing")
    public ResponseEntity<?> startEditing(
            @PathVariable Long documentId,
            HttpServletRequest httpRequest) {
        
        try {
            User user = getCurrentUser(httpRequest);
            log.info("편집 시작 요청 - 문서 ID: {}, 사용자: {}", documentId, user.getId());
            
            Document document = documentService.startEditing(documentId, user);
            
            log.info("편집 시작 성공 - 문서 ID: {}, 상태: {}", documentId, document.getStatus());
            return ResponseEntity.ok(DocumentResponse.from(document));
        } catch (Exception e) {
            log.error("편집 시작 실패 - 문서 ID: {}, 오류: {}", documentId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/{documentId}/complete-editing")
    public ResponseEntity<?> completeEditing(
            @PathVariable Long documentId,
            HttpServletRequest httpRequest) {
        
        try {
            User user = getCurrentUser(httpRequest);
            log.info("편집 완료 요청 - 문서 ID: {}, 사용자: {}", documentId, user.getId());
            
            // 문서 존재 확인
            Document document = documentService.getDocumentById(documentId)
                    .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다"));
            log.info("문서 상태 확인 - 현재 상태: {}", document.getStatus());
            
            Document updatedDocument = documentService.completeEditing(documentId, user);
            
            log.info("편집 완료 성공 - 문서 ID: {}, 새 상태: {}", documentId, updatedDocument.getStatus());
            return ResponseEntity.ok(DocumentResponse.from(updatedDocument));
        } catch (Exception e) {
            log.error("편집 완료 실패 - 문서 ID: {}, 오류: {}", documentId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/{documentId}/approve")
    public ResponseEntity<DocumentResponse> approveDocument(
            @PathVariable Long documentId,
            @RequestBody Map<String, Object> requestBody,
            HttpServletRequest httpRequest) {
        
        User user = getCurrentUser(httpRequest);
        
        String signatureData = (String) requestBody.get("signatureData");
        
        Document document = documentService.approveDocument(documentId, user, signatureData);
        
        return ResponseEntity.ok(DocumentResponse.from(document));
    }
    
    @PostMapping("/{documentId}/reject")
    public ResponseEntity<DocumentResponse> rejectDocument(
            @PathVariable Long documentId,
            @RequestBody Map<String, Object> requestBody,
            HttpServletRequest httpRequest) {
        
        User user = getCurrentUser(httpRequest);
        
        String reason = (String) requestBody.get("reason");
        
        Document document = documentService.rejectDocument(documentId, user, reason);
        
        return ResponseEntity.ok(DocumentResponse.from(document));
    }
    
    @GetMapping("/{documentId}/can-review")
    public ResponseEntity<Boolean> canReview(@PathVariable Long documentId, HttpServletRequest httpRequest) {
        try {
            User user = getCurrentUser(httpRequest);
            boolean canReview = documentService.canReview(documentId, user);
            return ResponseEntity.ok(canReview);
        } catch (Exception e) {
            log.error("Error checking review permission for document {}", documentId, e);
            return ResponseEntity.ok(false);
        }
    }
    
    @GetMapping("/{documentId}/history")
    public ResponseEntity<List<DocumentHistoryResponse>> getDocumentHistory(@PathVariable Long documentId, HttpServletRequest httpRequest) {
        try {
            User user = getCurrentUser(httpRequest);
            List<DocumentHistoryResponse> history = documentHistoryService.getDocumentHistory(documentId, user);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Error getting document history for document {}", documentId, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    private User getCurrentUser(HttpServletRequest request) {
        try {
            log.info("=== JWT 토큰 추출 시작 ===");
            
            // 모든 헤더 로깅 (디버깅용)
            java.util.Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String headerValue = request.getHeader(headerName);
                log.info("Header - {}: {}", headerName, headerValue);
            }
            
            // Authorization 헤더 확인
            String authHeader = request.getHeader("Authorization");
            log.info("Authorization 헤더: {}", authHeader);
            
            if (authHeader == null) {
                log.warn("Authorization 헤더가 없습니다");
                throw new RuntimeException("Authorization 헤더가 없습니다");
            }
            
            if (!authHeader.startsWith("Bearer ")) {
                log.warn("Bearer 토큰 형식이 아닙니다: {}", authHeader);
                throw new RuntimeException("Bearer 토큰 형식이 아닙니다");
            }
            
            // JWT 토큰에서 사용자 정보 추출 시도
            User user = authUtil.getCurrentUser(request);
//            log.info("JWT 토큰에서 추출된 사용자: {} ({})", user.getName(), user.getId());
            log.info("JWT 토큰에서 추출된 사용자: {} ({})", user.getName(), user.getEmail());
            return user;
        } catch (Exception e) {
            log.error("JWT 토큰 추출 실패: {}", e.getMessage(), e);
            log.warn("JWT 토큰 추출 실패, 인증이 필요합니다: {}", e.getMessage());
            // 인증이 필요한 상황에서는 예외를 던져서 클라이언트가 로그인하도록 유도
            throw new RuntimeException("인증이 필요합니다. 로그인 후 다시 시도해주세요.");
        }
    }
} 