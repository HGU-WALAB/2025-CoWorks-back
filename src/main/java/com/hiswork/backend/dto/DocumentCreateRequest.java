package com.hiswork.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DocumentCreateRequest {
    
    @NotNull(message = "템플릿 ID는 필수입니다")
    private Long templateId;
    private String title;
    private String editorEmail; // 편집자 이메일
    private String stagingId;   // 엑셀 업로드 후 스테이징 ID
} 