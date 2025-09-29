package com.hiswork.backend.repository;

import com.hiswork.backend.domain.Notification;
import com.hiswork.backend.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    // 특정 사용자의 알림 조회 (최신순)
    List<Notification> findByRecipientUserOrderByCreatedAtDesc(User recipientUser);
    
    // 특정 사용자의 알림 조회 (페이지네이션)
    Page<Notification> findByRecipientUserOrderByCreatedAtDesc(User recipientUser, Pageable pageable);
    
    // 특정 사용자의 읽지 않은 알림 조회
    List<Notification> findByRecipientUserAndIsReadFalseOrderByCreatedAtDesc(User recipientUser);
    
    // 특정 사용자의 읽지 않은 알림 개수
    long countByRecipientUserAndIsReadFalse(User recipientUser);
    
    // 특정 사용자의 최근 N개 알림 조회
    @Query("SELECT n FROM Notification n WHERE n.recipientUser = :user ORDER BY n.createdAt DESC LIMIT :limit")
    List<Notification> findRecentNotifications(@Param("user") User user, @Param("limit") int limit);
}