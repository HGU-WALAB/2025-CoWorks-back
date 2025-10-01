package com.hiswork.backend.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "templates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Template {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    private Boolean isPublic;
    
    private String pdfFilePath;
    
    private String pdfImagePath;

    @Column(length = 3000)
    private String pdfImagePaths;

    private String pdfPagesData; // PDF 페이지별 데이터 (JSON 형태)
    private Boolean isMultiPage;
    private Integer totalPages;
    
    @Column(columnDefinition = "TEXT")
    private String coordinateFields; // JSON 형태로 저장된 좌표 필드 정보
    
    // 이 템플릿으로 생성되는 문서의 만료일 (선택적)
    private LocalDateTime deadline;
    
    // 이 템플릿으로 생성되는 문서의 기본 폴더 (선택적)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_folder_id")
    @JsonIgnore
    private Folder defaultFolder;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    @JsonIgnore
    private User createdBy;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
} 