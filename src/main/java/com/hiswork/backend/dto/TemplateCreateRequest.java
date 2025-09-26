package com.hiswork.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateCreateRequest {
    
    @NotBlank(message = "템플릿 이름은 필수입니다")
    private String name;
    
    private String description;
    
    private Boolean isPublic;
    
    private String pdfFilePath;
    
    private String pdfImagePath;
    
    private String coordinateFields; // JSON 형태의 좌표 필드 정보
    
    private LocalDateTime deadline; // 만료일 (편집자가 문서 편집을 완료해야 하는 날짜)
    
    private UUID defaultFolderId; // 기본 폴더 ID (선택적)
} 