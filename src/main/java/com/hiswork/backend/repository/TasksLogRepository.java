package com.hiswork.backend.repository;

import com.hiswork.backend.domain.TasksLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TasksLogRepository extends JpaRepository<TasksLog, Long> {
    
    List<TasksLog> findByDocumentIdOrderByCreatedAtDesc(Long documentId);
    
    List<TasksLog> findByDocumentIdAndAssignedUserIdOrderByCreatedAtDesc(Long documentId, String assignedUserId);
    
    boolean existsByDocumentIdAndAssignedUserId(Long documentId, String assignedUserId);

    // 임시 유저 정보 할당
    List<TasksLog> findByDocumentIdAndAssignedUserIsNull(Long documentId);
} 