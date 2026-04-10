package com.trello.controller;

import com.trello.entity.Task;
import com.trello.entity.TaskComment;
import com.trello.entity.User;
import com.trello.service.TaskService;
import com.trello.service.TaskCommentService;
import com.trello.service.ProjectService;
import com.trello.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.beans.PropertyEditorSupport;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.bind.WebDataBinder;

/**
 * TaskPageController - Xử lý Request-Response cho các trang Task (HTML/Thymeleaf)
 * 
 * Cấp độ 1: GET - Truyền dữ liệu từ Java sang HTML (Model)
 * Cấp độ 2: POST - Nhận dữ liệu từ HTML Form
 */
@Controller
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskPageController {

    private final TaskService taskService;
    private final TaskCommentService taskCommentService;
    private final ProjectService projectService;
    private final UserService userService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(LocalDateTime.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                if (text == null || text.isEmpty()) {
                    setValue(null);
                } else {
                    setValue(LocalDate.parse(text).atStartOfDay());
                }
            }
        });
    }

    /**
     * Đảm bảo session.user luôn được set cho tất cả request trong controller này
     */
    @ModelAttribute
    public void ensureSessionUser(Authentication authentication, HttpSession session) {
        if (authentication != null && session.getAttribute("user") == null) {
            try {
                Long userId = extractUserId(authentication);
                if (userId != null) {
                    User user = userService.getUserById(userId);
                    session.setAttribute("user", user);
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Bước 1: GET /tasks - Trả về trang danh sách công việc
     * Model: Truyền danh sách tasks từ Java sang HTML
     */
    @GetMapping
    public String listTasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long projectId,
            Model model,
            Authentication authentication) {
        try {
            Long userId = extractUserId(authentication);
            if (userId == null) {
                return "redirect:/login";
            }
            
            // Lấy danh sách công việc (có thể filter)
            List<Task> tasks = taskService.getTasksAssignedToUser(userId);
            
            // Filter nếu có parameters từ form
            if (status != null && !status.isEmpty()) {
                tasks = tasks.stream()
                        .filter(t -> t.getStatus().equalsIgnoreCase(status))
                        .toList();
            }
            if (priority != null && !priority.isEmpty()) {
                tasks = tasks.stream()
                        .filter(t -> t.getPriority().equalsIgnoreCase(priority))
                        .toList();
            }
            if (projectId != null) {
                tasks = tasks.stream()
                        .filter(t -> t.getProject() != null && t.getProject().getId().equals(projectId))
                        .toList();
            }
            
            // Bước 2: Đổ dữ liệu vào Model - HTML truy cập qua ${tasks}
            model.addAttribute("tasks", tasks);
            model.addAttribute("taskCount", tasks.size());
            model.addAttribute("selectedStatus", status);
            model.addAttribute("selectedPriority", priority);
            
            return "task/list";  // Return template task/list.html
            
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi khi tải dữ liệu: " + e.getMessage());
            return "task/list";
        }
    }

    /**
     * GET /tasks/create - Trả về form tạo công việc mới
     * Model: Truyền empty Task object + danh sách projects
     */
    @GetMapping("/create")
    public String createTaskForm(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String status,
            Model model, Authentication authentication) {
        try {
            Long userId = extractUserId(authentication);
            if (userId == null) {
                return "redirect:/login";
            }
            Task task = new Task();
            if (projectId != null) task.setProject(projectService.getProjectById(projectId));
            if (status != null) task.setStatus(status);
            
            model.addAttribute("task", task);
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equalsIgnoreCase(a.getAuthority()) || "ADMIN".equalsIgnoreCase(a.getAuthority()));
            model.addAttribute("projects", isAdmin ? projectService.getAllProjects() : projectService.getProjectsByUserId(userId));
            model.addAttribute("users", userService.getAllUsers());
            model.addAttribute("isEditMode", false);
            model.addAttribute("presetProjectId", projectId);
            model.addAttribute("presetStatus", status);
            return "task/form";
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi: " + e.getMessage());
            return "task/form";
        }
    }

    /**
     * Bước 3: POST /tasks - Nhận dữ liệu từ HTML Form
     * @ModelAttribute: Spring map form fields → Task object
     */
    @PostMapping
    public String storeTask(
            @ModelAttribute Task task,
            Authentication authentication,
            Model model) {
        try {
            Long userId = extractUserId(authentication);
            if (userId == null) {
                return "redirect:/login";
            }
            
            // Set thông tin meta
            task.setUser(userService.getUserById(userId));
            task.setCreatedAt(LocalDateTime.now());
            if (task.getStatus() == null || task.getStatus().isEmpty()) {
                task.setStatus("TODO");
            }
            
            // Lưu vào Database
            Task savedTask = taskService.createTask(task);
            
            // Redirect tới trang chi tiết công việc
            return "redirect:/tasks/" + savedTask.getId();
            
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi khi tạo công việc: " + e.getMessage());
            model.addAttribute("task", task);
            model.addAttribute("isEditMode", false);
            return "task/form";
        }
    }

    /**
     * GET /tasks/{id} - Trả về trang chi tiết công việc
     * Model: Truyền task + comments → HTML
     */
    @GetMapping("/{id}")
    public String viewTask(
            @PathVariable Long id,
            Model model,
            Authentication authentication) {
        try {
            // Lấy thông tin công việc
            Task task = taskService.getTaskById(id);
            if (task == null) {
                model.addAttribute("error", "Công việc không tồn tại");
                return "redirect:/tasks";
            }
            
            // Lấy danh sách bình luận
            List<TaskComment> comments = taskCommentService.getCommentsByTaskId(id);
            
            // Đổ dữ liệu vào Model
            model.addAttribute("task", task);
            model.addAttribute("comments", comments);
            model.addAttribute("commentCount", comments.size());
            
            // Lấy thông tin dự án
            var project = task.getProject();
            model.addAttribute("project", project);
            
            return "task/detail";  // Task detail view
            
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi khi tải công việc: " + e.getMessage());
            return "redirect:/tasks";
        }
    }

    /**
     * GET /tasks/{id}/edit - Trả về form chỉnh sửa công việc
     */
    @GetMapping("/{id}/edit")
    public String editTaskForm(
            @PathVariable Long id,
            Model model,
            Authentication authentication) {
        try {
            Long userId = extractUserId(authentication);
            if (userId == null) {
                return "redirect:/login";
            }
            
            Task task = taskService.getTaskById(id);
            if (task == null) {
                return "redirect:/tasks";
            }
            
            model.addAttribute("task", task);
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equalsIgnoreCase(a.getAuthority()) || "ADMIN".equalsIgnoreCase(a.getAuthority()));
            model.addAttribute("projects", isAdmin ? projectService.getAllProjects() : projectService.getProjectsByUserId(userId));
            model.addAttribute("users", userService.getAllUsers());
            model.addAttribute("isEditMode", true);
            return "task/form";
            
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/tasks";
        }
    }

    /**
     * POST /tasks/{id}/update - Cập nhật công việc
     */
    @PostMapping("/{id}/update")
    public String updateTask(
            @PathVariable Long id,
            @ModelAttribute Task task,
            Model model) {
        try {
            task.setId(id);
            taskService.updateTask(task);
            
            return "redirect:/tasks/" + id;
            
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi khi cập nhật: " + e.getMessage());
            model.addAttribute("task", task);
            model.addAttribute("isEditMode", true);
            return "task/form";
        }
    }

    /**
     * POST /tasks/{id}/comments - Thêm bình luận
     * Form submission từ HTML comment form
     */
    @PostMapping("/{id}/comments")
    public String addComment(
            @PathVariable Long id,
            @RequestParam String content,
            Authentication authentication,
            Model model) {
        try {
            Long userId = extractUserId(authentication);
            if (userId == null) {
                return "redirect:/login";
            }
            
            TaskComment comment = new TaskComment();
            comment.setTask(taskService.getTaskById(id));
            comment.setUser(userService.getUserById(userId));
            comment.setContent(content);
            comment.setCreatedAt(LocalDateTime.now());
            
            taskCommentService.createComment(comment);
            
            // Quay lại trang chi tiết task
            return "redirect:/tasks/" + id;
            
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi khi thêm bình luận: " + e.getMessage());
            return "redirect:/tasks/" + id;
        }
    }

    /**
     * POST /tasks/{id}/status - Cập nhật status công việc (AJAX hoặc Form)
     */
    @PostMapping("/{id}/status")
    public String updateTaskStatus(
            @PathVariable Long id,
            @RequestParam String status,
            Model model) {
        try {
            Task task = taskService.getTaskById(id);
            if (task != null) {
                task.setStatus(status);
                taskService.updateTask(task);
            }
            
            return "redirect:/tasks/" + id;
            
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/tasks/" + id;
        }
    }

    /**
     * GET /tasks/{id}/delete - Xóa công việc
     */
    @GetMapping("/{id}/delete")
    public String deleteTask(@PathVariable Long id) {
        try {
            taskService.deleteTask(id);
            return "redirect:/tasks";
        } catch (Exception e) {
            return "redirect:/tasks/" + id;
        }
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
