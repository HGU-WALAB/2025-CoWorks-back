package com.hiswork.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DocumentCreateRequest {
    
    @NotNull(message = "템플릿 ID는 필수입니다")
    private Long templateId;
    
    private String editorEmail; // 편집자 이메일 (선택사항)
} 