package com.trello.service;

import com.trello.entity.Task;
import com.trello.entity.TaskNotificationLog;
import com.trello.entity.User;
import com.trello.repository.TaskNotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Service gửi thông báo task quá hạn qua email
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final MailService mailService;
    private final TaskNotificationLogRepository notificationLogRepository;

    private static final DateTimeFormatter DATE_FORMATTER = 
            DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy");
    private static final String NOTIFICATION_TYPE_OVERDUE = "OVERDUE";
    private static final String NOTIFICATION_TYPE_UPCOMING = "UPCOMING";

    /**
     * Kiểm tra xem đã gửi notification cho task trong 24h gần nhất
     */
    public boolean hasRecentNotification(Task task, String notificationType) {
        LocalDateTime oneDayAgo = LocalDateTime.now().minusHours(24);
        Optional<TaskNotificationLog> existing = 
                notificationLogRepository.findByTaskAndNotificationTypeAndSentAtAfter(
                        task, notificationType, oneDayAgo
                );
        return existing.isPresent();
    }

    /**
     * Gửi thông báo task quá hạn
     */
    public boolean sendOverdueNotification(Task task) {
        if (task.getAssignee() == null || task.getAssignee().getEmail() == null) {
            log.warn("Task [{}] có không assignee hoặc email", task.getId());
            return false;
        }

        if (hasRecentNotification(task, NOTIFICATION_TYPE_OVERDUE)) {
            log.debug("Task [{}] đã gửi OVERDUE notification trong 24h", task.getId());
            return false;
        }

        User assignee = task.getAssignee();
        String subject = String.format("[Trello Mini] ⚠️ Công việc đã quá hạn: %s", task.getTitle());
        
        String text = String.format("""
Xin chào %s,

Công việc "%s" của dự án "%s" đã quá hạn.

Chi tiết:
- Hạn chót: %s
- Độ ưu tiên: %s
- Trạng thái: %s

Vui lòng ưu tiên hoàn thành hoặc cập nhật tiến độ ngay!

Trân trọng,
Trello Mini - Hệ thống quản lý dự án
                """,
                assignee.getUsername() != null ? assignee.getUsername() : assignee.getName(),
                task.getTitle(),
                task.getProject() != null ? task.getProject().getName() : "Dự án không xác định",
                task.getDueDate() != null ? task.getDueDate().format(DATE_FORMATTER) : "Chưa xác định",
                task.getPriority() != null ? task.getPriority() : "MEDIUM",
                task.getStatus() != null ? task.getStatus() : "PENDING"
        );

        try {
            mailService.sendMail(assignee.getEmail(), subject, text);

            // Lưu log
            TaskNotificationLog notificationLog = TaskNotificationLog.builder()
                    .task(task)
                    .assignee(assignee)
                    .notificationType(NOTIFICATION_TYPE_OVERDUE)
                    .recipientEmail(assignee.getEmail())
                    .sentAt(LocalDateTime.now())
                    .status("SENT")
                    .build();
            notificationLogRepository.save(notificationLog);

            log.info("Gửi email OVERDUE cho {} - Task [{}]", assignee.getEmail(), task.getId());
            return true;
        } catch (Exception e) {
            log.error("Lỗi gửi email OVERDUE cho Task [{}]: {}", task.getId(), e.getMessage());
            
            TaskNotificationLog notificationLog = TaskNotificationLog.builder()
                    .task(task)
                    .assignee(assignee)
                    .notificationType(NOTIFICATION_TYPE_OVERDUE)
                    .recipientEmail(assignee.getEmail())
                    .sentAt(LocalDateTime.now())
                    .status("FAILED")
                    .build();
            notificationLogRepository.save(notificationLog);
            return false;
        }
    }

    /**
     * Gửi thông báo task sắp đến hạn 
     */
    public boolean sendUpcomingNotification(Task task) {
        if (task.getAssignee() == null || task.getAssignee().getEmail() == null) {
            log.warn("Task [{}] không có assignee hoặc email", task.getId());
            return false;
        }

        if (hasRecentNotification(task, NOTIFICATION_TYPE_UPCOMING)) {
            log.debug("Task [{}] đã gửi UPCOMING notification trong 24h", task.getId());
            return false;
        }

        User assignee = task.getAssignee();
        String subject = String.format("[Trello Mini] ⏰ Công việc sắp đến hạn: %s", task.getTitle());
        
        String text = String.format("""
Xin chào %s,

Công việc "%s" của dự án "%s" sắp đến hạn.

Chi tiết:
- Hạn chót: %s
- Độ ưu tiên: %s
- Trạng thái: %s

Vui lòng kiểm tra và chuẩn bị hoàn thành đúng hạn!

Trân trọng,
Trello Mini - Hệ thống quản lý dự án
                """,
                assignee.getUsername() != null ? assignee.getUsername() : assignee.getName(),
                task.getTitle(),
                task.getProject() != null ? task.getProject().getName() : "Dự án không xác định",
                task.getDueDate() != null ? task.getDueDate().format(DATE_FORMATTER) : "Chưa xác định",
                task.getPriority() != null ? task.getPriority() : "MEDIUM",
                task.getStatus() != null ? task.getStatus() : "PENDING"
        );

        try {
            mailService.sendMail(assignee.getEmail(), subject, text);

            // Lưu log
            TaskNotificationLog notificationLog = TaskNotificationLog.builder()
                    .task(task)
                    .assignee(assignee)
                    .notificationType(NOTIFICATION_TYPE_UPCOMING)
                    .recipientEmail(assignee.getEmail())
                    .sentAt(LocalDateTime.now())
                    .status("SENT")
                    .build();
            notificationLogRepository.save(notificationLog);

            log.info("Gửi email UPCOMING cho {} - Task [{}]", assignee.getEmail(), task.getId());
            return true;
        } catch (Exception e) {
            log.error("Lỗi gửi email UPCOMING cho Task [{}]: {}", task.getId(), e.getMessage());
            
            TaskNotificationLog notificationLog = TaskNotificationLog.builder()
                    .task(task)
                    .assignee(assignee)
                    .notificationType(NOTIFICATION_TYPE_UPCOMING)
                    .recipientEmail(assignee.getEmail())
                    .sentAt(LocalDateTime.now())
                    .status("FAILED")
                    .build();
            notificationLogRepository.save(notificationLog);
            return false;
        }
    }
}
