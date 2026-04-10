package com.trello.controller;

import com.trello.dto.ApiResponse;
import com.trello.dto.TaskRequest;
import com.trello.dto.TaskResponse;
import com.trello.entity.Task;
import com.trello.entity.TaskComment;
import com.trello.entity.User;
import com.trello.entity.Project;
import com.trello.service.TaskService;
import com.trello.service.TaskCommentService;
import com.trello.service.UserService;
import com.trello.service.ProjectService;
import com.trello.scheduler.TaskDeadlineNotifier;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final TaskCommentService taskCommentService;
    private final UserService userService;
    private final ProjectService projectService;
    private final TaskDeadlineNotifier taskDeadlineNotifier;

    /**
     * POST /api/projects/{projectId}/tasks - Tạo Task mới trong Project
     */
    @PostMapping("/{projectId}/tasks")
    public ResponseEntity<ApiResponse<?>> createTask(
            @PathVariable Long projectId,
            @Valid @RequestBody TaskRequest request,
            Authentication authentication) {
        try {
            Long userId = extractUserId(authentication);
            if (userId == null) {
                return new ResponseEntity<>(ApiResponse.builder()
                        .success(false)
                        .message("Vui lòng đăng nhập")
                        .statusCode(HttpStatus.UNAUTHORIZED.value())
                        .build(), HttpStatus.UNAUTHORIZED);
            }
            User user = userService.getUserById(userId);
            Project project = projectService.getProjectById(projectId);
            User assignee = null;
            if (request.getAssigneeId() != null) {
            assignee = userService.getUserById(request.getAssigneeId());
            }
            Task task = Task.builder()
                .project(project)
                .title(request.getTitle())
                .description(request.getDescription())
                .status(request.getStatus() != null ? request.getStatus() : "To do")
                .priority(request.getPriority() != null ? request.getPriority() : "Medium")
                .assignee(assignee)
                .user(user)
                .dueDate(request.getDueDate())
                .createdAt(LocalDateTime.now())
                .build();

            Task createdTask = taskService.createTask(task);
            TaskResponse taskResponse = convertToResponse(createdTask);

            ApiResponse<?> response = ApiResponse.builder()
                .success(true)
                .message("Tạo công việc thành công")
                .statusCode(HttpStatus.CREATED.value())
                .data(taskResponse)
                .build();

            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            ApiResponse<?> response = ApiResponse.builder()
                .success(false)
                .message("Lỗi khi tạo công việc: " + e.getMessage())
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .build();
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * PUT /api/tasks/{taskId} - Cập nhật Task (toàn bộ nội dung)
     */
        @PreAuthorize("@taskSecurity.canEditTask(authentication, #taskId) or hasRole('ADMIN')")
        @PutMapping("/{projectId}/tasks/{taskId}")
        public ResponseEntity<ApiResponse<?>> updateTask(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @Valid @RequestBody TaskRequest request,
            Authentication authentication) {
        try {
            Task task = taskService.getTaskById(taskId);

            if (task == null) {
                ApiResponse<?> response = ApiResponse.builder()
                        .success(false)
                        .message("Công việc không tồn tại")
                        .statusCode(HttpStatus.NOT_FOUND.value())
                        .build();
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }


            task.setTitle(request.getTitle());
            task.setDescription(request.getDescription());
            User assignee = null;
            if (request.getAssigneeId() != null) {
                assignee = userService.getUserById(request.getAssigneeId());
            }
            task.setAssignee(assignee);
            task.setPriority(request.getPriority());
            task.setDueDate(request.getDueDate());
            // task.setUpdatedAt(LocalDateTime.now());

            Task updatedTask = taskService.updateTask(task);
            TaskResponse taskResponse = convertToResponse(updatedTask);

            ApiResponse<?> response = ApiResponse.builder()
                    .success(true)
                    .message("Cập nhật công việc thành công")
                    .statusCode(HttpStatus.OK.value())
                    .data(taskResponse)
                    .build();

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            ApiResponse<?> response = ApiResponse.builder()
                    .success(false)
                    .message("Lỗi khi cập nhật công việc: " + e.getMessage())
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * PATCH /api/tasks/{taskId}/status - Chỉ cập nhật trạng thái Task
     */
    @PatchMapping("/{projectId}/tasks/{taskId}/status")
    public ResponseEntity<ApiResponse<?>> updateTaskStatus(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @RequestParam String status) {
        try {
            // Validate status
            if (!isValidStatus(status)) {
                ApiResponse<?> response = ApiResponse.builder()
                        .success(false)
                        .message("Trạng thái không hợp lệ. Chỉ cho phép: TODO, DOING, DONE")
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .build();
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            Task task = taskService.getTaskById(taskId);

            if (task == null) {
                ApiResponse<?> response = ApiResponse.builder()
                        .success(false)
                        .message("Công việc không tồn tại")
                        .statusCode(HttpStatus.NOT_FOUND.value())
                        .build();
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            task.setStatus(status);
            task.setUpdatedAt(LocalDateTime.now());

            Task updatedTask = taskService.updateTask(task);
            TaskResponse taskResponse = convertToResponse(updatedTask);

            ApiResponse<?> response = ApiResponse.builder()
                    .success(true)
                    .message("Cập nhật trạng thái công việc thành công")
                    .statusCode(HttpStatus.OK.value())
                    .data(taskResponse)
                    .build();

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            ApiResponse<?> response = ApiResponse.builder()
                    .success(false)
                    .message("Lỗi khi cập nhật trạng thái: " + e.getMessage())
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * DELETE /api/tasks/{taskId} - Xóa Task
     */
        @PreAuthorize("@taskSecurity.canEditTask(authentication, #taskId) or hasRole('ADMIN')")
        @DeleteMapping("/{projectId}/tasks/{taskId}")
        public ResponseEntity<ApiResponse<?>> deleteTask(
            @PathVariable Long projectId,
            @PathVariable Long taskId) {
        try {
            taskService.deleteTask(taskId);

            ApiResponse<?> response = ApiResponse.builder()
                    .success(true)
                    .message("Xóa công việc thành công")
                    .statusCode(HttpStatus.OK.value())
                    .build();

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            ApiResponse<?> response = ApiResponse.builder()
                    .success(false)
                    .message("Lỗi khi xóa công việc: " + e.getMessage())
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * POST /api/tasks/{taskId}/comments - Thêm bình luận vào Task
     */
    @PostMapping("/{projectId}/tasks/{taskId}/comments")
    public ResponseEntity<ApiResponse<?>> addComment(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @RequestParam String content,
            Authentication authentication) {
        try {
            if (content == null || content.trim().isEmpty()) {
                ApiResponse<?> response = ApiResponse.builder()
                        .success(false)
                        .message("Nội dung bình luận không được để trống")
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .build();
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            Long userId = extractUserId(authentication);
            if (userId == null) {
                return new ResponseEntity<>(ApiResponse.builder()
                        .success(false)
                        .message("Vui lòng đăng nhập")
                        .statusCode(HttpStatus.UNAUTHORIZED.value())
                        .build(), HttpStatus.UNAUTHORIZED);
            }
            Task task = taskService.getTaskById(taskId);
            User user = userService.getUserById(userId);
            TaskComment comment = new TaskComment();
            comment.setTask(task);
            comment.setUser(user);
            comment.setContent(content);
            comment.setCreatedAt(LocalDateTime.now());

            TaskComment createdComment = taskCommentService.createComment(comment);

            ApiResponse<?> response = ApiResponse.builder()
                    .success(true)
                    .message("Thêm bình luận thành công")
                    .statusCode(HttpStatus.CREATED.value())
                    .data(createdComment)
                    .build();

            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            ApiResponse<?> response = ApiResponse.builder()
                    .success(false)
                    .message("Lỗi khi thêm bình luận: " + e.getMessage())
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /api/projects/{projectId}/tasks - Lấy danh sách công việc trong Project
     */
    @GetMapping("/{projectId}/tasks")
    public ResponseEntity<ApiResponse<?>> getProjectTasks(@PathVariable Long projectId) {
        try {
            List<Task> tasks = taskService.getTasksByProjectId(projectId);
            List<TaskResponse> taskResponses = tasks.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            ApiResponse<?> response = ApiResponse.builder()
                    .success(true)
                    .message("Lấy danh sách công việc thành công")
                    .statusCode(HttpStatus.OK.value())
                    .data(taskResponses)
                    .build();

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            ApiResponse<?> response = ApiResponse.builder()
                    .success(false)
                    .message("Lỗi khi lấy danh sách công việc: " + e.getMessage())
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /api/tasks/{taskId}/comments - Lấy danh sách bình luận của Task
     */
    @GetMapping("/{projectId}/tasks/{taskId}/comments")
    public ResponseEntity<ApiResponse<?>> getTaskComments(@PathVariable Long taskId) {
        try {
            List<TaskComment> comments = taskCommentService.getCommentsByTaskId(taskId);

            ApiResponse<?> response = ApiResponse.builder()
                    .success(true)
                    .message("Lấy danh sách bình luận thành công")
                    .statusCode(HttpStatus.OK.value())
                    .data(comments)
                    .build();

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            ApiResponse<?> response = ApiResponse.builder()
                    .success(false)
                    .message("Lỗi khi lấy bình luận: " + e.getMessage())
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean isValidStatus(String status) {
        return status != null && (status.equals("TODO") || status.equals("DOING") || status.equals("DONE"));
    }

    private TaskResponse convertToResponse(Task task) {
        return TaskResponse.builder()
            .id(task.getId())
            .projectId(task.getProject() != null ? task.getProject().getId() : null)
            .title(task.getTitle())
            .description(task.getDescription())
            .status(task.getStatus())
            .priority(task.getPriority())
            .assigneeId(task.getAssignee() != null ? task.getAssignee().getId() : null)
            .userId(task.getUser() != null ? task.getUser().getId() : null)
            .createdAt(task.getCreatedAt())
            .updatedAt(task.getUpdatedAt())
            .dueDate(task.getDueDate())
            .build();
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null) return null;
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            try {
                Object principal = authentication.getPrincipal();
                if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User) {
                    org.springframework.security.oauth2.core.user.OAuth2User oauth2User = 
                        (org.springframework.security.oauth2.core.user.OAuth2User) principal;
                    String email = oauth2User.getAttribute("email");
                    if (email != null) {
                        User user = userService.findUserByEmail(email);
                        if (user != null) return user.getId();
                    }
                }
            } catch (Exception ex) {
                System.out.println("Error extracting userId from OAuth2: " + ex.getMessage());
            }
        }
        return null;
    }
}
