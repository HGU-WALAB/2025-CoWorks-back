package com.hiswork.backend.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bulk_staging_item")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkStagingItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staging_id", nullable = false)
    private BulkStaging staging;
    
    @Column(name = "row_number", nullable = false)
    private int rowNumber;
    
    @Column(name = "student_id", length = 50)
    private String studentId;
    
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    @Column(name = "email", nullable = false, length = 255)
    private String email;
    
    @Column(name = "course", length = 100)
    private String course;
    
    @Column(name = "document_title", length = 255)
    private String documentTitle;
    
    @Column(name = "is_valid", nullable = false)
    @Builder.Default
    private boolean isValid = true;
    
    @Column(name = "validation_error", length = 500)
    private String validationError;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status")
    @Builder.Default
    private ProcessingStatus processingStatus = ProcessingStatus.PENDING;
    
    @Column(name = "processing_reason", length = 500)
    private String processingReason;
    
    @Column(name = "created_document_id")
    private Long createdDocumentId;
    
    public enum ProcessingStatus {
        PENDING,    // 처리 대기
        CREATED,    // 문서 생성 완료
        SKIPPED,    // 중복 등으로 건너뜀
        FAILED      // 처리 실패
    }
    
    public boolean canProcess() {
        return isValid && processingStatus == ProcessingStatus.PENDING;
    }
}
