package com.hiswork.backend.repository;

import com.hiswork.backend.domain.DocumentRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRoleRepository extends JpaRepository<DocumentRole, Long> {
    
    List<DocumentRole> findByDocumentId(Long documentId);
    
    @Query("SELECT dr FROM DocumentRole dr WHERE dr.document.id = :documentId AND dr.taskRole = :taskRole")
    Optional<DocumentRole> findByDocumentAndRole(@Param("documentId") Long documentId, @Param("taskRole") DocumentRole.TaskRole taskRole);
    
    @Query("SELECT dr FROM DocumentRole dr WHERE dr.document.id = :documentId AND dr.assignedUser.id = :userId")
    Optional<DocumentRole> findByDocumentAndUser(@Param("documentId") Long documentId, @Param("userId") UUID userId);
    
    @Query("SELECT dr FROM DocumentRole dr WHERE dr.document.id = :documentId AND dr.assignedUser.id = :userId AND dr.taskRole = :taskRole")
    Optional<DocumentRole> findByDocumentAndUserAndRole(@Param("documentId") Long documentId, @Param("userId") UUID userId, @Param("taskRole") DocumentRole.TaskRole taskRole);
} 