package com.hiswork.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkDocumentResponse {
    
    private Long templateId;
    private int totalRows;
    private int created;
    private int skipped;
    private int failed;
    private List<BulkDocumentItem> items;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkDocumentItem {
        private int row;
        private String id; // 학번
        private String name; // 이름
        private String subject; // 과목
        private String email; // 이메일
        private String title;
        private String status; // CREATED, SKIPPED, FAILED
        private String reason; // 실패/스킵 이유
        private Long documentId; // 생성된 문서 ID (CREATED인 경우)
    }
}
