package com.hiswork.backend.controller;

import com.hiswork.backend.domain.Notification;
import com.hiswork.backend.domain.NotificationType;
import com.hiswork.backend.domain.User;
import com.hiswork.backend.service.NotificationService;
import com.hiswork.backend.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class NotificationController {
    
    private final NotificationService notificationService;
    private final AuthUtil authUtil;
    
    /**
     * 사용자의 알림 목록 조회 (페이지네이션)
     */
    @GetMapping
    public ResponseEntity<Page<Notification>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        
        try {
            User currentUser = authUtil.getCurrentUser(request);
            Page<Notification> notifications = notificationService.getUserNotifications(currentUser, page, size);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            log.error("알림 목록 조회 실패", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 읽지 않은 알림 목록 조회
     */
    @GetMapping("/unread")
    public ResponseEntity<List<Notification>> getUnreadNotifications(HttpServletRequest request) {
        try {
            User currentUser = authUtil.getCurrentUser(request);
            List<Notification> unreadNotifications = notificationService.getUnreadNotifications(currentUser);
            return ResponseEntity.ok(unreadNotifications);
        } catch (Exception e) {
            log.error("읽지 않은 알림 조회 실패", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 읽지 않은 알림 개수 조회
     */
    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Long>> getUnreadNotificationCount(HttpServletRequest request) {
        try {
            User currentUser = authUtil.getCurrentUser(request);
            long count = notificationService.getUnreadNotificationCount(currentUser);
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            log.error("읽지 않은 알림 개수 조회 실패", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 알림 읽음 처리
     */
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Map<String, String>> markAsRead(
            @PathVariable Long notificationId,
            HttpServletRequest request) {
        
        try {
            User currentUser = authUtil.getCurrentUser(request);
            notificationService.markAsRead(notificationId, currentUser);
            return ResponseEntity.ok(Map.of("message", "알림이 읽음 처리되었습니다."));
        } catch (Exception e) {
            log.error("알림 읽음 처리 실패: notificationId={}", notificationId, e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 모든 알림 읽음 처리
     */
    @PutMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead(HttpServletRequest request) {
        try {
            User currentUser = authUtil.getCurrentUser(request);
            notificationService.markAllAsRead(currentUser);
            return ResponseEntity.ok(Map.of("message", "모든 알림이 읽음 처리되었습니다."));
        } catch (Exception e) {
            log.error("모든 알림 읽음 처리 실패", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 알림 삭제
     */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Map<String, String>> deleteNotification(
            @PathVariable Long notificationId,
            HttpServletRequest request) {
        
        try {
            User currentUser = authUtil.getCurrentUser(request);
            notificationService.deleteNotification(notificationId, currentUser);
            return ResponseEntity.ok(Map.of("message", "알림이 삭제되었습니다."));
        } catch (Exception e) {
            log.error("알림 삭제 실패: notificationId={}", notificationId, e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 모든 알림 삭제
     */
    @DeleteMapping("/all")
    public ResponseEntity<Map<String, String>> deleteAllNotifications(HttpServletRequest request) {
        try {
            User currentUser = authUtil.getCurrentUser(request);
            notificationService.deleteAllNotifications(currentUser);
            return ResponseEntity.ok(Map.of("message", "모든 알림이 삭제되었습니다."));
        } catch (Exception e) {
            log.error("모든 알림 삭제 실패", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * SSE 연결 (실시간 알림)
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications(
            @RequestParam(value = "token", required = false) String token,
            HttpServletRequest request) {
        try {
            User currentUser;
            
            // 토큰이 쿼리 파라미터로 전달된 경우 처리
            if (token != null && !token.isEmpty()) {
                // Authorization 헤더에 토큰을 설정하여 AuthUtil이 처리할 수 있도록 함
                request.setAttribute("Authorization", "Bearer " + token);
            }
            
            currentUser = authUtil.getCurrentUser(request);
            log.info("SSE 연결 요청: 사용자={}", currentUser.getName());
            return notificationService.createSseEmitter(currentUser.getId());
        } catch (Exception e) {
            log.error("SSE 연결 실패", e);
            SseEmitter emitter = new SseEmitter();
            emitter.completeWithError(e);
            return emitter;
        }
    }
}