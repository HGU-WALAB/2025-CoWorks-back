package com.hiswork.backend.repository;

import com.hiswork.backend.domain.DocumentRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRoleRepository extends JpaRepository<DocumentRole, Long> {
    
    List<DocumentRole> findByDocumentId(Long documentId);
    
    @Query("SELECT dr FROM DocumentRole dr WHERE dr.document.id = :documentId AND dr.taskRole = :taskRole")
    Optional<DocumentRole> findByDocumentAndRole(@Param("documentId") Long documentId, @Param("taskRole") DocumentRole.TaskRole taskRole);
    
    @Query("SELECT dr FROM DocumentRole dr WHERE dr.document.id = :documentId AND dr.assignedUserId = :userId")
    Optional<DocumentRole> findByDocumentAndUser(@Param("documentId") Long documentId, @Param("userId") String userId);
    
    @Query("SELECT dr FROM DocumentRole dr WHERE dr.document.id = :documentId AND dr.assignedUserId = :userId AND dr.taskRole = :taskRole")
    Optional<DocumentRole> findByDocumentAndUserAndRole(@Param("documentId") Long documentId, @Param("userId") String userId, @Param("taskRole") DocumentRole.TaskRole taskRole);
    
    // 임시 유저 할당 (assignedUserId에 저장)
    @Query("SELECT dr FROM DocumentRole dr WHERE dr.assignedUserId = :pendingUserId AND dr.pendingEmail IS NOT NULL")
    List<DocumentRole> findByPendingUserId(@Param("pendingUserId") String pendingUserId);

    @Query("SELECT dr FROM DocumentRole dr WHERE dr.pendingEmail = :pendingEmail AND dr.pendingEmail IS NOT NULL")
    List<DocumentRole> findByPendingEmail(@Param("pendingEmail") String pendingEmail);
} 