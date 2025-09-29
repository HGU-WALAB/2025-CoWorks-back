package com.hiswork.backend.domain;

public enum NotificationType {
    DOCUMENT_ASSIGNED("문서 할당"),
    DOCUMENT_COMPLETED("문서 완료"),
    DOCUMENT_DEADLINE("마감일 알림"),
    DOCUMENT_UPDATED("문서 업데이트"),
    SYSTEM_NOTICE("시스템 공지");
    
    private final String description;
    
    NotificationType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}