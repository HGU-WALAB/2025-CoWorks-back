package com.hiswork.backend.dto;

import com.hiswork.backend.domain.User;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.List;

public class MailRequest {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MailNotificationRequest {
        private String to;
        private String message;
    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MailFailureRequest {
        private String course;
        private String username;
        private Long failedCount;
        private String batchId;
        private List<FailureRow> failureRows;
        private String retryLink;
        private String dashboardLink;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EditorAssignmentEmailCommand {
        Long documentId;
        String documentTitle;
        String creatorName;
        String editorEmail;
        String editorName;
        ZonedDateTime dueDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewerAssignmentEmailCommand {
        Long documentId;
        String documentTitle;
        String editorName;
        String reviewerEmail;
        String reviewerName;
        String reviewScope;
        ZonedDateTime reviewDueDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RejectionAssignmentEmailCommand {
        Long documentId;
        String documentTitle;
        String editorEmail;
        String editorName;
        String rejecterName;
        String rejectionReason;
        ZonedDateTime dueDate;
    }
}
