package com.hiswork.backend.repository;

import com.hiswork.backend.domain.DocumentStatusLog;
import com.hiswork.backend.domain.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentStatusLogRepository extends JpaRepository<DocumentStatusLog, Long> {
    
    /**
     * 특정 문서의 모든 상태 로그를 시간순으로 조회
     */
    List<DocumentStatusLog> findByDocumentOrderByTimestampAsc(Document document);
    
    /**
     * 특정 문서와 상태에 대한 최신 로그 조회
     */
    Optional<DocumentStatusLog> findTopByDocumentAndStatusOrderByTimestampDesc(Document document, Document.DocumentStatus status);
    
    /**
     * 특정 문서의 최신 상태 로그 조회
     */
    Optional<DocumentStatusLog> findTopByDocumentOrderByTimestampDesc(Document document);
    
    /**
     * 특정 문서 ID의 모든 상태 로그를 시간순으로 조회
     */
    @Query("SELECT sl FROM DocumentStatusLog sl WHERE sl.document.id = :documentId ORDER BY sl.timestamp ASC")
    List<DocumentStatusLog> findByDocumentIdOrderByTimestampAsc(@Param("documentId") Long documentId);
}