package com.hiswork.backend.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.hiswork.backend.domain.Document;
import com.hiswork.backend.domain.DocumentRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {
    private Long id;
    private Long templateId;
    private String templateName;
    private String title;
    private JsonNode data;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deadline;
    private List<TaskInfo> tasks;
    private List<DocumentStatusLogResponse> statusLogs;
    
    // Template 정보 추가
    private TemplateInfo template;
    
    public static DocumentResponse from(Document document) {
        List<TaskInfo> taskInfos = document.getDocumentRoles().stream()
                .map(TaskInfo::from)
                .collect(Collectors.toList());
        
        List<DocumentStatusLogResponse> statusLogResponses = document.getStatusLogs().stream()
                .map(DocumentStatusLogResponse::from)
                .collect(Collectors.toList());
        
        TemplateInfo templateInfo = TemplateInfo.from(document.getTemplate());
        
        return DocumentResponse.builder()
                .id(document.getId())
                .templateId(document.getTemplate().getId())
                .templateName(document.getTemplate().getName())
                .title(document.getTitle())
                .data(document.getData())
                .status(document.getStatus().name())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .deadline(document.getDeadline())
                .tasks(taskInfos)
                .statusLogs(statusLogResponses)
                .template(templateInfo)
                .build();
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskInfo {
        private Long id;
        private String role;
        private String assignedUserName;
        private String assignedUserEmail;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        
        public static TaskInfo from(DocumentRole documentRole) {
            return TaskInfo.builder()
                    .id(documentRole.getId())
                    .role(documentRole.getTaskRole().name())
                    .assignedUserName(documentRole.getPendingName())
                    .assignedUserEmail(documentRole.getPendingEmail())
                    .createdAt(documentRole.getCreatedAt())
                    .updatedAt(documentRole.getUpdatedAt())
                    .build();
        }
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateInfo {
        private Long id;
        private String name;
        private String description;
        private Boolean isPublic;
        private String pdfFilePath;
        private String pdfImagePath;
        private String coordinateFields; // JSON 형태로 저장된 좌표 필드 정보
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        
        public static TemplateInfo from(com.hiswork.backend.domain.Template template) {
            return TemplateInfo.builder()
                    .id(template.getId())
                    .name(template.getName())
                    .description(template.getDescription())
                    .isPublic(template.getIsPublic())
                    .pdfFilePath(template.getPdfFilePath())
                    .pdfImagePath(template.getPdfImagePath())
                    .coordinateFields(template.getCoordinateFields())
                    .createdAt(template.getCreatedAt())
                    .updatedAt(template.getUpdatedAt())
                    .build();
        }
    }
} 