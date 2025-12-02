package com.hiswork.backend.service;

import com.hiswork.backend.domain.Document;
import com.hiswork.backend.domain.DocumentRole;
import com.hiswork.backend.domain.User;
import com.hiswork.backend.dto.MailRequest;
import com.hiswork.backend.repository.DocumentRepository;
import com.hiswork.backend.repository.DocumentRoleRepository;
import com.hiswork.backend.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 문서 마감일 알림 스케줄러
 * 매일 오전 9시에 실행되어 마감일이 하루 남은 작성 중(EDITING) 문서의 편집자에게 알림 메일 발송
 */
@Service
@RequiredArgsConstructor
public class DeadlineReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(DeadlineReminderScheduler.class);

    private final DocumentRepository documentRepository;
    private final DocumentRoleRepository documentRoleRepository;
    private final UserRepository userRepository;
    private final MailService mailService;

    /**
     * 마감일 임박 알림 (매일 오전 9시 실행)
     * 작성 단계(EDITING)이고 마감일이 하루 남은 문서의 편집자에게 알림 메일 발송
     */
    @Scheduled(cron = "0 0 9 * * ?")
    @Transactional(readOnly = true)
    public void sendDeadlineReminders() {
        log.info("마감일 임박 알림 스케줄러 시작");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrow = now.plusDays(1);
        
        // 내일이 마감일인 범위 (내일 00:00 ~ 23:59)
        LocalDateTime tomorrowStart = tomorrow.toLocalDate().atStartOfDay();
        LocalDateTime tomorrowEnd = tomorrow.toLocalDate().atTime(23, 59, 59);

        // EDITING 상태이고 마감일이 내일인 문서 조회
        List<Document> documents = documentRepository.findByStatusAndDeadlineBetween(
            Document.DocumentStatus.EDITING,
            tomorrowStart,
            tomorrowEnd
        );

        log.info("마감일 임박 문서 {}개 발견", documents.size());

        int sentCount = 0;
        for (Document document : documents) {
            try {
                // 편집자(EDITOR) 역할을 가진 사용자 찾기
                List<DocumentRole> editorRoles = documentRoleRepository
                    .findByDocumentAndRole(document.getId(), DocumentRole.TaskRole.EDITOR)
                    .map(List::of)
                    .orElse(List.of());

                for (DocumentRole role : editorRoles) {
                    // assignedUserId로 사용자 조회
                    if (role.getAssignedUserId() == null) {
                        continue; // 등록된 사용자가 없으면 스킵
                    }
                    
                    User editor = userRepository.findById(role.getAssignedUserId())
                        .orElse(null);
                    
                    if (editor == null) {
                        continue; // 사용자를 찾을 수 없으면 스킵
                    }
                    
                    // 알림 메일 발송
                    mailService.sendDeadlineReminderNotification(
                        MailRequest.DeadlineReminderEmailCommand.builder()
                            .documentId(document.getId())
                            .documentTitle(document.getTitle())
                            .editorEmail(editor.getEmail())
                            .editorName(editor.getName())
                            .deadline(document.getDeadline() != null ? 
                                document.getDeadline().atZone(ZoneId.systemDefault()) : null)
                            .build()
                    );
                    
                    sentCount++;
                    log.info("마감일 알림 발송 완료 - 문서: {}, 편집자: {}", 
                        document.getTitle(), editor.getEmail());
                }
                
            } catch (Exception e) {
                log.error("마감일 알림 발송 실패 - 문서 ID: {}, 오류: {}", 
                    document.getId(), e.getMessage(), e);
            }
        }

        log.info("마감일 임박 알림 스케줄러 완료 - 총 {}개 메일 발송", sentCount);
    }
}
