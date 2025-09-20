package com.hiswork.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BulkCommitResponse {
    private int created;
    private int skipped;
    private int failed;
    private List<CommitItem> items;
    
    @Data
    @Builder
    public static class CommitItem {
        private int row;
        private String studentId;
        private String name;
        private String email;
        private String course;
        private String documentTitle;
        private CommitStatus status;
        private String reason;
        private Long documentId; // 생성된 문서 ID (CREATED인 경우)
        
        public enum CommitStatus {
            CREATED,  // 문서 생성 완료
            SKIPPED,  // 건너뜀
            FAILED    // 실패
        }
    }
}
