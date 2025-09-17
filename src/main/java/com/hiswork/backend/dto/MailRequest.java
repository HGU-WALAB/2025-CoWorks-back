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
        String projectName;
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
        String projectName;
        String documentTitle;
        String editorName;
        String reviewerEmail;
        String reviewerName;
        String reviewScope;
        ZonedDateTime reviewDueDate;
    }
}
