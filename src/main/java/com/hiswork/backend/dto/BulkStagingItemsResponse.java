package com.hiswork.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BulkStagingItemsResponse {
    private String stagingId;
    private String templateName;
    private String originalFilename;
    private int totalRows;
    private int validRows;
    private int invalidRows;
    private List<StagingItem> items;
    
    @Data
    @Builder
    public static class StagingItem {
        private int rowNumber;
        private String studentId;
        private String name;
        private String email;
        private String course;
        private String documentTitle;
        private boolean isValid;
        private String validationError;
        private String processingStatus;
        private String processingReason;
        private Long createdDocumentId;
        private UserStatus userStatus; // REGISTERED, UNREGISTERED
        
        public enum UserStatus {
            REGISTERED,   // 이미 등록된 사용자
            UNREGISTERED  // 미등록 사용자 (가입 후 자동 할당 예정)
        }
    }
}
