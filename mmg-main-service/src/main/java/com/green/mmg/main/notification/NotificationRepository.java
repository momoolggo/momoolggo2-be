package com.green.mmg.main.notification;

import com.green.mmg.main.notification.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserNoOrderByCreatedAtDesc(Long userNo, Pageable pageable);

    Optional<Notification> findByNotificationIdAndUserNo(Long notificationId, Long userNo);

    List<Notification> findByUserNoAndReadFalse(Long userNo);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.userNo = :userNo")
    int deleteByUserNo(@Param("userNo") Long userNo);
}