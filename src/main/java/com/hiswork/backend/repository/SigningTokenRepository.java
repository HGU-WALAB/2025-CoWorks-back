package com.hiswork.backend.repository;

import com.hiswork.backend.domain.SigningToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SigningTokenRepository extends JpaRepository<SigningToken, String> {
    
    /**
     * 사용되지 않은 유효한 토큰 조회
     */
    Optional<SigningToken> findByTokenAndUsedFalse(String token);
    
    /**
     * 문서 ID와 서명자 이메일로 토큰 조회
     */
    Optional<SigningToken> findByDocumentIdAndSignerEmail(Long documentId, String signerEmail);
    
    /**
     * 만료된 토큰 목록 조회
     */
    @Query("SELECT st FROM SigningToken st WHERE st.expiresAt < :now AND st.used = false")
    List<SigningToken> findExpiredTokens(@Param("now") LocalDateTime now);
    
    /**
     * 오래된 사용 완료 토큰 삭제
     */
    void deleteByExpiresAtBeforeAndUsedTrue(LocalDateTime date);
    
    /**
     * 문서 ID로 모든 토큰 조회
     */
    List<SigningToken> findByDocumentId(Long documentId);
}
