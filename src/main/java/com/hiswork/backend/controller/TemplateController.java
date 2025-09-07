package com.hiswork.backend.controller;

import com.hiswork.backend.domain.Template;
import com.hiswork.backend.domain.User;
import com.hiswork.backend.domain.Position;
import com.hiswork.backend.domain.Role;
import com.hiswork.backend.dto.TemplateCreateRequest;
import com.hiswork.backend.dto.TemplateResponse;
import com.hiswork.backend.service.TemplateService;
import com.hiswork.backend.service.PdfService;
import com.hiswork.backend.repository.UserRepository;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class TemplateController {
    
    private final TemplateService templateService;
    private final PdfService pdfService;
    private final UserRepository userRepository;
    private final AuthUtil authUtil;
    
//    @PostMapping
//    public ResponseEntity<?> createTemplate(
//            @Valid @RequestBody TemplateCreateRequest request,
//            HttpServletRequest httpRequest) {
//
//        try {
//            User user = getCurrentUser(httpRequest);
//            Template template = Template.builder()
//                    .name(request.getName())
//                    .description(request.getDescription())
//                    .isPublic(request.getIsPublic())
//                    .pdfFilePath(request.getPdfFilePath())
//                    .pdfImagePath(request.getPdfImagePath())
//                    .coordinateFields(request.getCoordinateFields())  // 추가
//                    .createdBy(user)
//                    .build();
//            template = templateService.savePdfTemplate(template);
//
//            log.info("템플릿 생성 성공: {} by {}", template.getName(), user.getEmail());
//            return ResponseEntity.status(HttpStatus.CREATED)
//                    .body(TemplateResponse.from(template));
//        } catch (Exception e) {
//            log.error("템플릿 생성 실패: {}", e.getMessage(), e);
//            return ResponseEntity.badRequest()
//                    .body(Map.of("error", e.getMessage()));
//        }
//    }
    
    @PostMapping("/upload-pdf")
    public ResponseEntity<?> uploadPdfTemplate(
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String templateName,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "isPublic", defaultValue = "false") Boolean isPublic,
            @RequestParam(value = "coordinateFields", required = false) String coordinateFields,
            HttpServletRequest httpRequest) {
        
        try {
            User user = getCurrentUser(httpRequest);
            
            // PDF 파일 업로드 및 이미지 변환
            PdfService.PdfUploadResult uploadResult = pdfService.uploadPdfTemplate(file);
            
            // PDF 기반 템플릿 생성
            Template template = Template.builder()
                    .name(templateName)
                    .description(description)
                    .isPublic(isPublic)
                    .pdfFilePath(uploadResult.getPdfFilePath())
                    .pdfImagePath(uploadResult.getPdfImagePath())
                    .coordinateFields(coordinateFields)  // coordinateFields 추가
                    .createdBy(user)
                    .build();
            
            Template savedTemplate = templateService.savePdfTemplate(template);
            
            log.info("PDF 템플릿 생성 성공: {} by {}", savedTemplate.getName(), user.getId());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "template", TemplateResponse.from(savedTemplate),
                            "pdfImagePath", uploadResult.getPdfImagePath(),
                            "originalFilename", uploadResult.getOriginalFilename()
                    ));
        } catch (Exception e) {
            log.error("PDF 템플릿 생성 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping
    public ResponseEntity<List<TemplateResponse>> getAllTemplates() {
        List<Template> templates = templateService.getAllTemplates();
        List<TemplateResponse> responses = templates.stream()
                .map(TemplateResponse::from)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<TemplateResponse> getTemplate(@PathVariable Long id) {
        return templateService.getTemplateById(id)
                .map(template -> ResponseEntity.ok(TemplateResponse.from(template)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody TemplateCreateRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            User user = getCurrentUser(httpRequest);
            Template template = templateService.updateTemplate(id, request, user);
            
            log.info("템플릿 수정 성공: {} by {}", template.getName(), user.getId());
            return ResponseEntity.ok(TemplateResponse.from(template));
        } catch (Exception e) {
            log.error("템플릿 수정 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTemplate(@PathVariable Long id, HttpServletRequest httpRequest) {
        try {
            User user = getCurrentUser(httpRequest);
            templateService.deleteTemplate(id, user);
            
            log.info("템플릿 삭제 성공: {} by {}", id, user.getId());
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("템플릿 삭제 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    private User getCurrentUser(HttpServletRequest request) {
        try {
            // JWT 토큰에서 사용자 정보 추출 시도
            return authUtil.getCurrentUser(request);
        } catch (Exception e) {
            log.warn("JWT 토큰 추출 실패, 기본 사용자 사용: {}", e.getMessage());
            // 개발 환경용 fallback: 기본 사용자 사용
            return getUserOrCreate("test@example.com", "Test User", "1234");
        }
    }
    
    private User getUserOrCreate(String email, String defaultName, String defaultPassword) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .name(defaultName)
                            .email(email)
                            .position(Position.교직원)
                            .role(Role.USER)
                            .build();
                    return userRepository.save(newUser);
                });
    }
} 