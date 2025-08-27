package com.hiswork.backend.repository;

import com.hiswork.backend.domain.TasksLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TasksLogRepository extends JpaRepository<TasksLog, Long> {
    
    List<TasksLog> findByDocumentIdOrderByCreatedAtDesc(Long documentId);
    
    List<TasksLog> findByDocumentIdAndAssignedUserIdOrderByCreatedAtDesc(Long documentId, UUID assignedUserId);
    
    boolean existsByDocumentIdAndAssignedUserEmail(Long documentId, String email);
} 