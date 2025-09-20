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
@Table(name = "documents_role")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentRole {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    @JsonIgnore
    private Document document;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_user_id", nullable = true)
    private User assignedUser;
    
    // 임시 유저 정보 할당 관련 필드 (assignedUser가 null인 경우 사용)
    @Column(name = "pending_user_id")
    private String pendingUserId;  // 미등록 사용자의 학번 
    
    @Column(name = "pending_email")
    private String pendingEmail;   // 미등록 사용자의 이메일
    
    @Column(name = "pending_name")
    private String pendingName;    // 미등록 사용자의 이름
    
    @Enumerated(EnumType.STRING)
    @Column(name = "task_role", nullable = false)
    private TaskRole taskRole;
    
    // 문서 할당 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_status", nullable = false)
    @Builder.Default
    private AssignmentStatus assignmentStatus = AssignmentStatus.ACTIVE;
    
    // 청구 상태 (가입 전 사용자의 문서 청구 상태)
    @Enumerated(EnumType.STRING)
    @Column(name = "claim_status", nullable = false)
    @Builder.Default
    private ClaimStatus claimStatus = ClaimStatus.CLAIMED;
    
    // 검토자 지정 권한 (편집자가 검토자를 지정할 수 있는지 여부)
    @Column(name = "can_assign_reviewer", nullable = false)
    @Builder.Default
    private Boolean canAssignReviewer = false;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    public enum TaskRole {
        CREATOR, EDITOR, REVIEWER
    }
    
    public enum AssignmentStatus {
        ACTIVE,   // 활성 할당 (등록된 사용자)
        PENDING   // 임시 할당 (미등록 사용자)
    }
    
    public enum ClaimStatus {
        PENDING,  // 청구 대기 (가입 전 사용자)
        CLAIMED   // 청구 완료 (가입 후 연결됨)
    }
} 