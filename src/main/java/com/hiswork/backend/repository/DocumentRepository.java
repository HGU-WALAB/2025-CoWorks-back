package com.hiswork.backend.repository;

import com.hiswork.backend.domain.Document;
import com.hiswork.backend.domain.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    
    @Query("SELECT d FROM Document d JOIN d.documentRoles dr WHERE " +
           "(dr.assignedUser.id = :userId AND dr.assignmentStatus = 'ACTIVE') OR " +
           "(dr.pendingUserId = :userId AND dr.assignmentStatus = 'PENDING') " +
           "ORDER BY d.createdAt DESC")
    List<Document> findDocumentsByUserId(@Param("userId") String userId);
    
    @Query("SELECT d FROM Document d JOIN d.documentRoles dr WHERE " +
           "(dr.assignedUser.email = :email AND dr.assignmentStatus = 'ACTIVE') OR " +
           "(dr.pendingEmail = :email AND dr.assignmentStatus = 'PENDING') " +
           "ORDER BY d.createdAt DESC")
    List<Document> findDocumentsByUserEmail(@Param("email") String email);
    
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
    
    /**
     * 제목으로 문서 존재 여부 확인
     */
    boolean existsByTitle(String title);
    
    /**
     * statusLogs를 함께 조회하는 메서드
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.statusLogs sl WHERE d.id = :id ORDER BY sl.timestamp ASC")
    Optional<Document> findByIdWithStatusLogs(@Param("id") Long id);
    
    /**
     * 사용자의 문서들을 statusLogs와 함께 조회
     */
    @Query("SELECT DISTINCT d FROM Document d " +
           "LEFT JOIN FETCH d.statusLogs sl " +
           "JOIN d.documentRoles dr WHERE " +
           "(dr.assignedUser.id = :userId AND dr.assignmentStatus = 'ACTIVE') OR " +
           "(dr.pendingUserId = :userId AND dr.assignmentStatus = 'PENDING') " +
           "ORDER BY d.createdAt DESC, sl.timestamp ASC")
    List<Document> findDocumentsByUserIdWithStatusLogs(@Param("userId") String userId);
} 