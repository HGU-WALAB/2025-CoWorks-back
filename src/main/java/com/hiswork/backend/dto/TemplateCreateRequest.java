package com.hiswork.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
} 