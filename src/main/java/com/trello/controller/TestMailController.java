package com.trello.controller;

import com.trello.scheduler.TaskDeadlineNotifier;
import com.trello.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Test Email Controller for Mailpit
 * Dùng để test chức năng gửi mail với Mailpit
 */
@RestController
@RequestMapping("/api/test/mail")
@RequiredArgsConstructor
@Slf4j
public class TestMailController {

    private final TaskDeadlineNotifier taskDeadlineNotifier;
    private final MailService mailService;

    /**
     * Trigger task deadline notification check
     * GET /api/test/mail/notify-deadlines
     */
    @GetMapping("/notify-deadlines")
    public ResponseEntity<?> triggerDeadlineNotification() {
        try {
            log.info("Triggering task deadline notification check...");
            taskDeadlineNotifier.checkAndNotifyDeadlines();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Task deadline notification check triggered successfully");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error triggering deadline notification", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error: " + e.getMessage()
            ));
        }
    }

    /**
     * Send a test email to verify Mailpit configuration
     * POST /api/test/mail/send?to=test@example.com
     */
    @PostMapping("/send")
    public ResponseEntity<?> sendTestEmail(@RequestParam String to) {
        try {
            log.info("Sending test email to: {}", to);
            
            String subject = "Test Email from Trello Mini";
            String body = "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "    <meta charset=\"UTF-8\">\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "    <h2>Test Email from Trello Mini</h2>\n" +
                    "    <p>Đây là email test để kiểm tra cấu hình Mailpit.</p>\n" +
                    "    <p><strong>Timestamp:</strong> " + System.currentTimeMillis() + "</p>\n" +
                    "    <p>Nếu bạn nhận được email này, Mailpit đã được cấu hình đúng!</p>\n" +
                    "</body>\n" +
                    "</html>";
            
            mailService.sendMail(to, subject, body);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Test email sent successfully to: " + to);
            response.put("timestamp", System.currentTimeMillis());
            
            log.info("Test email sent to: {}", to);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error sending test email", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error sending email: " + e.getMessage()
            ));
        }
    }

    /**
     * Send a test notification to multiple recipients
     * POST /api/test/mail/send-bulk?recipients=user1@example.com,user2@example.com
     */
    @PostMapping("/send-bulk")
    public ResponseEntity<?> sendBulkTestEmail(@RequestParam String recipients) {
        try {
            String[] emails = recipients.split(",");
            log.info("Sending bulk test emails to {} recipients", emails.length);
            
            String subject = "Bulk Test Email from Trello Mini";
            String body = "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "    <meta charset=\"UTF-8\">\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "    <h2>Bulk Test Email</h2>\n" +
                    "    <p>Đây là email test gửi hàng loạt (bulk) để kiểm tra hiệu suất.</p>\n" +
                    "    <p><strong>Timestamp:</strong> " + System.currentTimeMillis() + "</p>\n" +
                    "</body>\n" +
                    "</html>";
            
            int successCount = 0;
            int failCount = 0;
            
            for (String email : emails) {
                try {
                    mailService.sendMail(email.trim(), subject, body);
                    successCount++;
                    log.info("Sent email to: {}", email.trim());
                } catch (Exception e) {
                    failCount++;
                    log.error("Failed to send email to: {}", email.trim(), e);
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", failCount == 0);
            response.put("message", String.format("Sent %d emails, %d failed", successCount, failCount));
            response.put("successCount", successCount);
            response.put("failCount", failCount);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error sending bulk test emails", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error: " + e.getMessage()
            ));
        }
    }

    /**
     * Health check for mail service
     * GET /api/test/mail/health
     */
    @GetMapping("/health")
    public ResponseEntity<?> mailServiceHealth() {
        try {
            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("service", "Mail Service");
            health.put("timestamp", System.currentTimeMillis());
            health.put("mailhost", "localhost");
            health.put("mailport", 1025);
            
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "DOWN",
                "message", e.getMessage()
            ));
        }
    }
}
