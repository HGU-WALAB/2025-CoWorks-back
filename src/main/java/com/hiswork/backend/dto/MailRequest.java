package com.hiswork.backend.dto;

import com.hiswork.backend.domain.User;
import lombok.*;

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
}
