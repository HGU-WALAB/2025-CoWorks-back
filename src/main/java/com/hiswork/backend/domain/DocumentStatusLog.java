package com.hiswork.backend.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_status_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentStatusLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Document.DocumentStatus status;
    
    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "changed_by_email")
    private String changedByEmail;
    
    @Column(name = "changed_by_name")
    private String changedByName;
    
    private String comment; // 상태 변경 사유나 코멘트
}