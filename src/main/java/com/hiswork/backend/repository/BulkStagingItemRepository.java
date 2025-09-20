package com.hiswork.backend.repository;

import com.hiswork.backend.domain.BulkStagingItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BulkStagingItemRepository extends JpaRepository<BulkStagingItem, Long> {
    
    /**
     * 특정 스테이징의 모든 아이템 조회
     */
    @Query("SELECT bsi FROM BulkStagingItem bsi WHERE bsi.staging.stagingId = :stagingId ORDER BY bsi.rowNumber")
    List<BulkStagingItem> findByStagingIdOrderByRowNumber(@Param("stagingId") String stagingId);
    
    /**
     * 특정 스테이징의 처리 가능한 아이템들 조회
     */
    @Query("SELECT bsi FROM BulkStagingItem bsi WHERE bsi.staging.stagingId = :stagingId AND bsi.isValid = true AND bsi.processingStatus = 'PENDING' ORDER BY bsi.rowNumber")
    List<BulkStagingItem> findProcessableItems(@Param("stagingId") String stagingId);
    
    /**
     * 특정 스테이징의 유효한 아이템 수 조회
     */
    @Query("SELECT COUNT(bsi) FROM BulkStagingItem bsi WHERE bsi.staging.stagingId = :stagingId AND bsi.isValid = true")
    long countValidItems(@Param("stagingId") String stagingId);
    
    /**
     * 특정 스테이징의 처리 상태별 아이템 수 조회
     */
    @Query("SELECT bsi.processingStatus, COUNT(bsi) FROM BulkStagingItem bsi WHERE bsi.staging.stagingId = :stagingId GROUP BY bsi.processingStatus")
    List<Object[]> countByProcessingStatus(@Param("stagingId") String stagingId);
}
