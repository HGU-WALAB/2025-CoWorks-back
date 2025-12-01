package com.hiswork.backend.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "signing_tokens", indexes = {
    @Index(name = "idx_token_used", columnList = "token, used"),
    @Index(name = "idx_document_signer", columnList = "documentId, signerEmail"),
    @Index(name = "idx_expires_at", columnList = "expiresAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SigningToken {
    
    @Id
    @Column(length = 255)
    private String token;
    
    @Column(nullable = false)
    private Long documentId;
    
    @Column(nullable = false)
    private String signerEmail;
    
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(nullable = false)
    @Builder.Default
    private boolean used = false;
    
    @Column
    private LocalDateTime usedAt;
    
    @Column(length = 45)
    private String ipAddress;
    
    @Column(nullable = false)
    @Builder.Default
    private int accessCount = 0;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * 새로운 서명 토큰 생성
     */
    public static SigningToken create(Long documentId, String signerEmail, int expiryDays) {
        return SigningToken.builder()
                .token(UUID.randomUUID().toString())
                .documentId(documentId)
                .signerEmail(signerEmail)
                .expiresAt(LocalDateTime.now().plusDays(expiryDays))
                .build();
    }
    
    /**
     * 토큰 유효성 확인
     */
    public boolean isValid() {
        return !used && LocalDateTime.now().isBefore(expiresAt);
    }
    
    /**
     * 토큰 사용 완료 처리
     */
    public void markAsUsed() {
        this.used = true;
        this.usedAt = LocalDateTime.now();
    }
    
    /**
     * 접근 횟수 증가
     */
    public void incrementAccessCount() {
        this.accessCount++;
    }
}
