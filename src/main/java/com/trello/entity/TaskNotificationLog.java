package com.trello.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Lưu lịch sử gửi thông báo task quá hạn
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "task_notification_logs", 
       indexes = {@Index(name = "idx_task_id", columnList = "task_id"),
                  @Index(name = "idx_notification_type", columnList = "notification_type")})
public class TaskNotificationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne
    @JoinColumn(name = "assignee_id", nullable = false)
    private User assignee;

    private String notificationType; // "OVERDUE", "UPCOMING"
    private String recipientEmail;
    private LocalDateTime sentAt;
    private String status; // "SENT", "FAILED"
}
