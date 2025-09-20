package com.hiswork.backend.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "bulk_staging")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkStaging {
    
    @Id
    @Column(name = "staging_id", length = 36)
    private String stagingId; // UUID
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private Template template;
    
    @Column(name = "original_filename")
    private String originalFilename;
    
    @Column(name = "total_rows", nullable = false)
    private int totalRows;
    
    @Column(name = "valid_rows", nullable = false)
    private int validRows;
    
    @Column(name = "invalid_rows", nullable = false)
    private int invalidRows;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private StagingStatus status = StagingStatus.READY;
    
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "staging", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BulkStagingItem> items;
    
    public enum StagingStatus {
        READY,      // 업로드 완료, 커밋 대기
        COMMITTED,  // 문서 생성 완료
        CANCELED    // 사용자 취소
    }
    
    public boolean canCommit() {
        return status == StagingStatus.READY;
    }
    
    public boolean canCancel() {
        return status == StagingStatus.READY;
    }
}
