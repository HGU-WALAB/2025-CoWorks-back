package com.hiswork.backend.dto;

import com.hiswork.backend.domain.Template;
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
public class TemplateResponse {
    private Long id;
    private String name;
    private String description;
    private Boolean isPublic;
    private String pdfFilePath;
    private String pdfImagePath;
    private String pdfImagePaths;
    private String coordinateFields;  // 추가
    private LocalDateTime deadline;  // 만료일 추가
    private UUID defaultFolderId;  // 기본 폴더 ID
    private String defaultFolderName;  // 기본 폴더 이름
    private String createdById;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String pdfPagesData; // PDF 페이지별 데이터 (JSON 형태)
    private Boolean isMultiPage;
    private Integer totalPages;
    
    public static TemplateResponse from(Template template) {
        return TemplateResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .description(template.getDescription())
                .isPublic(template.getIsPublic())
                .pdfFilePath(template.getPdfFilePath())
                .pdfImagePath(template.getPdfImagePath())
                .pdfImagePaths(template.getPdfImagePaths())
                .coordinateFields(template.getCoordinateFields())  // 추가
                .deadline(template.getDeadline())  // 만료일 추가
                .defaultFolderId(template.getDefaultFolder() != null ? template.getDefaultFolder().getId() : null)
                .defaultFolderName(template.getDefaultFolder() != null ? template.getDefaultFolder().getName() : null)
                .createdById(template.getCreatedBy().getId())
                .createdByName(template.getCreatedBy().getName())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .pdfPagesData(template.getPdfPagesData())
                .isMultiPage(template.getIsMultiPage())
                .totalPages(template.getTotalPages())
                .build();
    }
} 