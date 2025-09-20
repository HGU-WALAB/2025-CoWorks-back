package com.hiswork.backend.repository;

import com.hiswork.backend.domain.BulkStaging;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BulkStagingRepository extends JpaRepository<BulkStaging, String> {
    
    /**
     * 특정 사용자가 생성한 스테이징 조회
     */
    @Query("SELECT bs FROM BulkStaging bs WHERE bs.stagingId = :stagingId AND bs.creator.id = :creatorId")
    Optional<BulkStaging> findByStagingIdAndCreatorId(@Param("stagingId") String stagingId, @Param("creatorId") String creatorId);
    
    /**
     * 특정 사용자의 활성 스테이징 조회 (READY 상태)
     */
    @Query("SELECT bs FROM BulkStaging bs WHERE bs.creator.id = :creatorId AND bs.status = 'READY' ORDER BY bs.createdAt DESC")
    List<BulkStaging> findActiveByCreatorId(@Param("creatorId") String creatorId);
    
    /**
     * 특정 기간 이전에 생성된 완료/취소 상태의 스테이징들 조회 (정리용)
     */
    @Query("SELECT bs FROM BulkStaging bs WHERE bs.status IN ('COMMITTED', 'CANCELED') AND bs.updatedAt < :before")
    List<BulkStaging> findCompletedBefore(@Param("before") LocalDateTime before);
}
