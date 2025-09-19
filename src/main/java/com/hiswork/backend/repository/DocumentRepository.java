package com.hiswork.backend.repository;

import com.hiswork.backend.domain.Document;
import com.hiswork.backend.domain.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    
    @Query("SELECT d FROM Document d JOIN d.documentRoles dr WHERE dr.assignedUser.id = :userId ORDER BY d.createdAt DESC")
    List<Document> findDocumentsByUserId(@Param("userId") String userId);
    
    /**
     * 특정 폴더에 속한 문서들 조회
     */
    List<Document> findByFolderOrderByCreatedAtDesc(Folder folder);
    
    /**
     * 폴더에 속하지 않은 문서들 조회 (미분류 문서)
     */
    List<Document> findByFolderIsNullOrderByCreatedAtDesc();
    
    /**
     * 특정 폴더의 문서 개수
     */
    long countByFolder(Folder folder);
    
    /**
     * 폴더에 속하지 않은 문서 개수
     */
    long countByFolderIsNull();
} 