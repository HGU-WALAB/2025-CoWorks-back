package com.hiswork.backend.service;

import com.hiswork.backend.domain.SigningToken;
import com.hiswork.backend.exception.InvalidTokenException;
import com.hiswork.backend.repository.SigningTokenRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SigningTokenService {
    
    private final SigningTokenRepository tokenRepository;
    private final MailService mailService;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;
    
    @Value("${app.signing-token.expiry-days:7}")
    private int expiryDays;
    
    /**
     * 서명 토큰 생성 및 이메일 발송
     */
    @Transactional
    public SigningToken createAndSendToken(Long documentId, String signerEmail, String signerName, String documentTitle) {
        log.info("서명 토큰 생성 시작 - 문서 ID: {}, 서명자: {}", documentId, signerEmail);
        
        // 기존 유효한 토큰이 있으면 재사용
        Optional<SigningToken> existingToken = tokenRepository
            .findByDocumentIdAndSignerEmail(documentId, signerEmail);
        
        if (existingToken.isPresent() && existingToken.get().isValid()) {
            log.info("기존 유효한 토큰 재사용 - 토큰: {}", existingToken.get().getToken());
            sendSigningEmail(existingToken.get(), signerName, documentTitle);
            return existingToken.get();
        }
        
        // 새 토큰 생성
        SigningToken token = SigningToken.create(documentId, signerEmail, expiryDays);
        tokenRepository.save(token);
        
        log.info("새 서명 토큰 생성 완료 - 토큰: {}, 만료일: {}", token.getToken(), token.getExpiresAt());
        
        // 이메일 발송
        sendSigningEmail(token, signerName, documentTitle);
        
        return token;
    }
    
    /**
     * 서명 요청 이메일 발송
     */
    private void sendSigningEmail(SigningToken token, String signerName, String documentTitle) {
        String signingUrl = String.format(
            "%s/email-sign/%d?token=%s",
            frontendUrl,
            token.getDocumentId(),
            token.getToken()
        );
        
        log.info("서명 요청 이메일 발송 - 수신자: {}, URL: {}", token.getSignerEmail(), signingUrl);
        
        try {
            mailService.sendSigningRequestEmail(
                token.getSignerEmail(),
                signerName,
                documentTitle,
                signingUrl,
                token.getExpiresAt()
            );
            log.info("서명 요청 이메일 발송 성공");
        } catch (Exception e) {
            log.error("서명 요청 이메일 발송 실패", e);
            // 이메일 발송 실패해도 토큰은 생성됨
        }
    }
    
    /**
     * 토큰 검증
     */
    @Transactional
    public SigningToken validateToken(String token, Long documentId) {
        log.info("토큰 검증 시작 - 토큰: {}, 문서 ID: {}", token, documentId);
        
        SigningToken signingToken = tokenRepository.findByTokenAndUsedFalse(token)
            .orElseThrow(() -> {
                log.warn("유효하지 않은 토큰: {}", token);
                return new InvalidTokenException("유효하지 않은 토큰입니다");
            });
        
        if (!signingToken.isValid()) {
            log.warn("만료된 토큰: {}, 만료일: {}", token, signingToken.getExpiresAt());
            throw new InvalidTokenException("만료된 토큰입니다");
        }
        
        if (!signingToken.getDocumentId().equals(documentId)) {
            log.warn("문서 ID 불일치 - 토큰의 문서: {}, 요청 문서: {}", 
                signingToken.getDocumentId(), documentId);
            throw new InvalidTokenException("문서 ID가 일치하지 않습니다");
        }
        
        // 접근 횟수 증가
        signingToken.incrementAccessCount();
        tokenRepository.save(signingToken);
        
        log.info("토큰 검증 성공 - 서명자: {}, 접근 횟수: {}", 
            signingToken.getSignerEmail(), signingToken.getAccessCount());
        
        return signingToken;
    }
    
    /**
     * 토큰 조회 (검증 없이)
     */
    public Optional<SigningToken> findTokenByValue(String token) {
        return tokenRepository.findById(token);
    }
    
    /**
     * 토큰 사용 완료 처리
     */
    @Transactional
    public void markTokenAsUsed(String token) {
        log.info("토큰 사용 완료 처리 - 토큰: {}", token);
        
        SigningToken signingToken = tokenRepository.findById(token)
            .orElseThrow(() -> new InvalidTokenException("토큰을 찾을 수 없습니다"));
        
        signingToken.markAsUsed();
        tokenRepository.save(signingToken);
        
        log.info("토큰 사용 완료 - 사용 시각: {}", signingToken.getUsedAt());
    }
    
    /**
     * 만료된 토큰 정리 (매일 새벽 2시)
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("만료된 토큰 정리 시작");
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneMonthAgo = now.minusMonths(1);
        
        // 사용된 토큰 중 1개월 이상 지난 것 삭제
        tokenRepository.deleteByExpiresAtBeforeAndUsedTrue(oneMonthAgo);
        
        log.info("만료된 토큰 정리 완료");
    }
}
