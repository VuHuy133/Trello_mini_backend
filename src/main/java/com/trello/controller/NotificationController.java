package com.trello.controller;

import com.trello.dto.ApiResponse;
import com.trello.scheduler.TaskDeadlineNotifier;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller để test gửi notification task
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class NotificationController {

    private final TaskDeadlineNotifier taskDeadlineNotifier;

    /**
     * POST /api/tasks/trigger-notification - Gửi thông báo task quá hạn (FOR TESTING)
     */
    @PostMapping("/trigger-notification")
    public ResponseEntity<ApiResponse<?>> triggerNotification() {
        try {
            taskDeadlineNotifier.checkAndNotifyDeadlines();
            ApiResponse<?> response = ApiResponse.builder()
                    .success(true)
                    .message("Notification check triggered successfully")
                    .statusCode(HttpStatus.OK.value())
                    .build();
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            ApiResponse<?> response = ApiResponse.builder()
                    .success(false)
                    .message("Error during notification check: " + e.getMessage())
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
