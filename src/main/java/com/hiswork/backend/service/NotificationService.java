package com.hiswork.backend.service;

import com.hiswork.backend.domain.Notification;
import com.hiswork.backend.domain.NotificationType;
import com.hiswork.backend.domain.User;
import com.hiswork.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    
    // SSE 연결 관리 (사용자ID -> SseEmitter)
    private final Map<String, SseEmitter> sseEmitters = new ConcurrentHashMap<>();
    
    /**
     * 새로운 알림 생성
     */
    public Notification createNotification(User recipient, String title, String message, 
                                         NotificationType type, Long relatedDocumentId, String actionUrl) {
        Notification notification = Notification.builder()
                .recipientUser(recipient)
                .title(title)
                .message(message)
                .type(type)
                .relatedDocumentId(relatedDocumentId)
                .actionUrl(actionUrl)
                .build();
        
        notification = notificationRepository.save(notification);
        
        // 실시간 알림 전송
        sendRealTimeNotification(recipient.getId(), notification);
        
        log.info("새 알림 생성: 사용자={}, 제목={}", recipient.getName(), title);
        
        return notification;
    }
    
    /**
     * 특정 사용자의 알림 목록 조회 (페이지네이션)
     */
    @Transactional(readOnly = true)
    public Page<Notification> getUserNotifications(User user, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return notificationRepository.findByRecipientUserOrderByCreatedAtDesc(user, pageable);
    }
    
    /**
     * 특정 사용자의 읽지 않은 알림 조회
     */
    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(User user) {
        return notificationRepository.findByRecipientUserAndIsReadFalseOrderByCreatedAtDesc(user);
    }
    
    /**
     * 특정 사용자의 읽지 않은 알림 개수 조회
     */
    @Transactional(readOnly = true)
    public long getUnreadNotificationCount(User user) {
        return notificationRepository.countByRecipientUserAndIsReadFalse(user);
    }
    
    /**
     * 알림을 읽음으로 표시
     */
    public Notification markAsRead(Long notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("알림을 찾을 수 없습니다: " + notificationId));
        
        // 권한 확인 - 알림의 수신자와 요청한 사용자가 같은지 확인
        if (!notification.getRecipientUser().getId().equals(user.getId())) {
            throw new RuntimeException("해당 알림에 접근할 권한이 없습니다.");
        }
        
        notification.markAsRead();
        return notificationRepository.save(notification);
    }
    
    /**
     * 모든 읽지 않은 알림을 읽음으로 표시
     */
    public void markAllAsRead(User user) {
        List<Notification> unreadNotifications = getUnreadNotifications(user);
        unreadNotifications.forEach(Notification::markAsRead);
        notificationRepository.saveAll(unreadNotifications);
        
        log.info("사용자 {}의 모든 알림을 읽음으로 처리: {}개", user.getName(), unreadNotifications.size());
    }
    
    /**
     * 알림 삭제
     */
    public void deleteNotification(Long notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("알림을 찾을 수 없습니다: " + notificationId));
        
        // 권한 확인 - 알림의 수신자와 요청한 사용자가 같은지 확인
        if (!notification.getRecipientUser().getId().equals(user.getId())) {
            throw new RuntimeException("해당 알림을 삭제할 권한이 없습니다.");
        }
        
        notificationRepository.delete(notification);
        log.info("알림 삭제 완료 - 사용자: {}, 알림ID: {}", user.getName(), notificationId);
    }
    
    /**
     * 특정 사용자의 모든 알림 삭제
     */
    public void deleteAllNotifications(User user) {
        List<Notification> userNotifications = notificationRepository.findByRecipientUserOrderByCreatedAtDesc(user);
        notificationRepository.deleteAll(userNotifications);
        
        log.info("사용자 {}의 모든 알림 삭제 완료: {}개", user.getName(), userNotifications.size());
    }
    
    /**
     * SSE 연결 생성
     */
    public SseEmitter createSseEmitter(String userId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        
        emitter.onCompletion(() -> {
            log.info("SSE 연결 완료: 사용자={}", userId);
            sseEmitters.remove(userId);
        });
        
        emitter.onTimeout(() -> {
            log.info("SSE 연결 타임아웃: 사용자={}", userId);
            sseEmitters.remove(userId);
        });
        
        emitter.onError((ex) -> {
            log.error("SSE 연결 오류: 사용자={}", userId, ex);
            sseEmitters.remove(userId);
        });
        
        sseEmitters.put(userId, emitter);
        
        try {
            // 연결 확인용 초기 메시지 전송
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("SSE 연결이 설정되었습니다"));
        } catch (IOException e) {
            log.error("SSE 초기 메시지 전송 실패: 사용자={}", userId, e);
            sseEmitters.remove(userId);
            emitter.completeWithError(e);
        }
        
        log.info("SSE 연결 생성: 사용자={}", userId);
        return emitter;
    }
    
    /**
     * 실시간 알림 전송
     */
    private void sendRealTimeNotification(String userId, Notification notification) {
        SseEmitter emitter = sseEmitters.get(userId);
        
        if (emitter != null) {
            try {
                // 현재 사용자의 읽지 않은 알림 개수 계산
                long unreadCount = getUnreadNotificationCount(notification.getRecipientUser());
                
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(Map.of(
                                "id", notification.getId(),
                                "title", notification.getTitle(),
                                "message", notification.getMessage(),
                                "type", notification.getType(),
                                "actionUrl", notification.getActionUrl(),
                                "createdAt", notification.getCreatedAt(),
                                "unreadCount", unreadCount
                        )));
                
                log.info("실시간 알림 전송 성공: 사용자={}, 알림ID={}, 읽지않은개수={}", userId, notification.getId(), unreadCount);
            } catch (IOException e) {
                log.error("실시간 알림 전송 실패: 사용자={}, 알림ID={}", userId, notification.getId(), e);
                sseEmitters.remove(userId);
            }
        } else {
            log.debug("SSE 연결이 없어 실시간 알림 전송 불가: 사용자={}", userId);
        }
    }
}