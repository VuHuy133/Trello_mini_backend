package com.trello.scheduler;

import com.trello.entity.Task;
import com.trello.repository.TaskRepository;
import com.trello.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaskDeadlineNotifier {

    private final TaskRepository taskRepository;
    private final NotificationService notificationService;

    /**
     * Chạy mỗi ngày lúc 8h sáng để kiểm tra và gửi thông báo task quá hạn
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void notifyTaskDeadlines() {
        log.info("=== Bắt đầu kiểm tra task quá hạn ===");
        try {
            List<Task> tasks = taskRepository.findAll();
            log.info("Tổng số task: {}", tasks.size());

            LocalDateTime now = LocalDateTime.now();
            int overdueCount = 0;
            int upcomingCount = 0;

            for (Task task : tasks) {
                try {
                    if (task.getDueDate() == null) continue;
                    if (task.getAssignee() == null) continue;

                    // Bỏ qua task đã hoàn thành
                    if ("DONE".equalsIgnoreCase(task.getStatus()) ||
                        "COMPLETED".equalsIgnoreCase(task.getStatus())) {
                        continue;
                    }

                    long hoursToDue = ChronoUnit.HOURS.between(now, task.getDueDate());
                    log.debug("Task [{}]: '{}' - Hạn: {}, Còn {} giờ, Người: {}",
                            task.getId(), task.getTitle(), task.getDueDate(), hoursToDue, 
                            task.getAssignee().getEmail());

                    // Sắp đến hạn (trong 24h tới)
                    if (hoursToDue > 0 && hoursToDue <= 24) {
                        if (notificationService.sendUpcomingNotification(task)) {
                            upcomingCount++;
                        }
                    }
                    // Đã quá hạn
                    else if (hoursToDue < 0) {
                        if (notificationService.sendOverdueNotification(task)) {
                            overdueCount++;
                        }
                    }
                } catch (Exception e) {
                    log.error("Lỗi xử lý task [{}]: {}", task.getId(), e.getMessage());
                }
            }
            log.info("=== Hoàn thành kiểm tra task ===");
            log.info("Email gửi đi - Quá hạn: {}, Sắp hạn: {}", overdueCount, upcomingCount);
        } catch (Exception e) {
            log.error("Lỗi kiểm tra task quá hạn: {}", e.getMessage(), e);
        }
    }

    /**
     * Gọi thủ công để kiểm tra ngay
     */
    public void checkAndNotifyDeadlines() {
        notifyTaskDeadlines();
    }
}
