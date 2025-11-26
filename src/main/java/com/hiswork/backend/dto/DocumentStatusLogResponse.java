package com.hiswork.backend.dto;

import com.hiswork.backend.domain.DocumentStatusLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentStatusLogResponse {
    private Long id;
    private String status;
    private LocalDateTime timestamp;
    private String changedByEmail;
    private String changedByName;
    private String comment;
    private Boolean rejectLog; // 반려 여부 (true: 반려, false: 일반 상태 변경)
    
    public static DocumentStatusLogResponse from(DocumentStatusLog statusLog) {
        return DocumentStatusLogResponse.builder()
                .id(statusLog.getId())
                .status(statusLog.getStatus().name())
                .timestamp(statusLog.getTimestamp())
                .changedByEmail(statusLog.getChangedByEmail())
                .changedByName(statusLog.getChangedByName())
                .comment(statusLog.getComment())
                .rejectLog(statusLog.getRejectLog() != null ? statusLog.getRejectLog() : false)
                .build();
    }
}