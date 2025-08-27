package com.hiswork.backend.repository;

import com.hiswork.backend.domain.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    
    @Query("SELECT d FROM Document d JOIN d.documentRoles dr WHERE dr.assignedUser.id = :userId ORDER BY d.createdAt DESC")
    List<Document> findDocumentsByUserId(@Param("userId") UUID userId);
} 