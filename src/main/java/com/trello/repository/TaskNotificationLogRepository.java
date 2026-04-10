package com.trello.repository;

import com.trello.entity.Task;
import com.trello.entity.TaskNotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TaskNotificationLogRepository extends JpaRepository<TaskNotificationLog, Long> {
    /**
     * Kiểm tra xem đã gửi notification cho task với type cụ thể trong 24h gần nhất
     */
    Optional<TaskNotificationLog> findByTaskAndNotificationTypeAndSentAtAfter(
            Task task, 
            String notificationType, 
            LocalDateTime fromTime
    );
}
