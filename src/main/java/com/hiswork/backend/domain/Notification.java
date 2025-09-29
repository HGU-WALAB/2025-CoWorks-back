package com.hiswork.backend.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // 알림을 받는 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    @JsonIgnore
    private User recipientUser;
    
    // 알림 제목
    @Column(name = "title", nullable = false, length = 200)
    private String title;
    
    // 알림 내용
    @Column(name = "message", nullable = false, length = 500)
    private String message;
    
    // 알림 타입 (문서 할당, 댓글, 마감일 등)
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType type;
    
    // 읽음 여부
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;
    
    // 관련 문서 ID (선택적)
    @Column(name = "related_document_id")
    private Long relatedDocumentId;
    
    // 관련 링크 URL (선택적)
    @Column(name = "action_url", length = 500)
    private String actionUrl;
    
    // 읽은 시간
    @Column(name = "read_at")
    private LocalDateTime readAt;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // 읽음 처리 메서드
    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }
}