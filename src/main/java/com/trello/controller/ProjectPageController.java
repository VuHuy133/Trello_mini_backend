package com.trello.controller;

import com.trello.entity.Project;
import com.trello.entity.Task;
import com.trello.entity.User;
import com.trello.service.ProjectMemberService;
import com.trello.service.ProjectService;
import com.trello.service.TaskService;
import com.trello.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ProjectPageController - Xử lý Request-Response cho các trang Project (HTML/Thymeleaf)
 * 
 * Cấp độ 1: GET - Truyền dữ liệu từ Java sang HTML (Model)
 * Cấp độ 2: POST - Nhận dữ liệu từ HTML Form
 */
@Controller
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectPageController {

    private final ProjectService projectService;
    private final TaskService taskService;
    private final ProjectMemberService projectMemberService;
    private final UserService userService;

    /**
     * Helper method: Extract userId from authentication
     * Handles both regular login (userId) and OAuth2 login (email)
     */
    private Long extractUserId(Authentication authentication) {
        if (authentication == null) return null;
        
        // Try to parse as Long (regular login)
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            // OAuth2 login - try to get email from OAuth2User
            try {
                Object principal = authentication.getPrincipal();
                if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User) {
                    org.springframework.security.oauth2.core.user.OAuth2User oauth2User = 
                        (org.springframework.security.oauth2.core.user.OAuth2User) principal;
                    String email = oauth2User.getAttribute("email");
                    if (email != null) {
                        User user = userService.findUserByEmail(email);
                        if (user != null) {
                            return user.getId();
                        }
                    }
                }
            } catch (Exception ex) {
                System.out.println("Error extracting userId from OAuth2: " + ex.getMessage());
            }
        }
        return null;
    }

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
     * Bước 1: GET /projects - Trả về trang danh sách dự án
     * Model: Truyền danh sách projects từ Java sang HTML
     */
    @GetMapping
    public String listProjects(Model model, Authentication authentication) {
        try {
            Long userId = extractUserId(authentication);
            if (userId == null) return "redirect:/login";
            
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equalsIgnoreCase(a.getAuthority()) || "ADMIN".equalsIgnoreCase(a.getAuthority()));

            // Lấy danh sách dự án theo quyền public/private
            List<Project> projects = isAdmin ? projectService.getAllProjects() : projectService.getProjectsByUserId(userId);

            // Tính task count, member count, membership status cho từng project
            Map<Long, Integer> taskCounts = new HashMap<>();
            Map<Long, Integer> memberCounts = new HashMap<>();
            Map<Long, Boolean> isMember = new HashMap<>();
            for (Project p : projects) {
                taskCounts.put(p.getId(), taskService.getTasksByProjectId(p.getId()).size());
                memberCounts.put(p.getId(), projectMemberService.getMembersOfProject(p.getId()).size());
                isMember.put(p.getId(), projectService.isProjectMember(p.getId(), userId));
            }

            model.addAttribute("projects", projects);
            model.addAttribute("projectCount", projects.size());
            model.addAttribute("taskCounts", taskCounts);
            model.addAttribute("memberCounts", memberCounts);
            model.addAttribute("isMember", isMember);
            model.addAttribute("currentUserId", userId);

            return "project/list";
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi khi tải dữ liệu: " + e.getMessage());
            return "project/list";
        }
    }

    /**
     * GET /projects/create - Trả về form tạo dự án mới
     * Model: Truyền empty Project object để bind form
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/create")
    public String createProjectForm(Model model, Authentication authentication) {
        model.addAttribute("project", new Project());
        return "project/create";
    }

    /**
     * Bước 3: POST /projects - Nhận dữ liệu từ HTML Form
     * @ModelAttribute: Spring tự động map form fields → Project object
     * Request: HTML gửi form data
     * Response: Redirect hoặc trả về trang kết quả
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public String storeProject(
            @ModelAttribute Project project,
            Authentication authentication,
            Model model) {
        try {
            Long userId = extractUserId(authentication);
            if (userId == null) return "redirect:/login";
            
            // Set owner và createdAt (không overwrite id để tránh update nhầm bản ghi)
            User owner = userService.getUserById(userId);
            project.setOwner(owner);
            project.setId(null);
            project.setCreatedAt(LocalDateTime.now());
            // Lưu vào Database
            Project savedProject = projectService.createProject(project);
            // Redirect tới trang chi tiết dự án vừa tạo
            return "redirect:/projects/" + savedProject.getId();
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi khi tạo dự án: " + e.getMessage());
            model.addAttribute("project", project);
            model.addAttribute("isEditMode", false);
            return "project/create";
        }
    }

    /**
     * GET /projects/{id} - Trả về trang chi tiết dự án (Kanban Board)
     * Model: Truyền project + danh sách tasks → HTML
     */
    @GetMapping("/{id}")
    public String viewProject(
            @PathVariable Long id,
            Model model,
            Authentication authentication) {
        try {
            Long userId = extractUserId(authentication);
            if (userId == null) return "redirect:/login";

            // Lấy thông tin dự án
            Project project = projectService.getProjectById(id);
            if (project == null) {
                model.addAttribute("error", "Dự án không tồn tại");
                return "redirect:/projects";
            }

            // Permission check: USER chỉ được xem PUBLIC hoặc PRIVATE nếu là member
            if (!projectService.canUserViewProject(id, userId)) {
                model.addAttribute("error", "Bạn không có quyền xem dự án này");
                return "redirect:/projects";
            }
            
            // Lấy danh sách công việc trong dự án
            List<Task> allTasks = taskService.getTasksByProjectId(id);
            
            // Phân loại tasks theo status (cho Kanban board)
            List<Task> todoTasks = allTasks.stream()
                    .filter(t -> "TODO".equalsIgnoreCase(t.getStatus()))
                    .toList();
            List<Task> doingTasks = allTasks.stream()
                    .filter(t -> "DOING".equalsIgnoreCase(t.getStatus()))
                    .toList();
            List<Task> doneTasks = allTasks.stream()
                    .filter(t -> "DONE".equalsIgnoreCase(t.getStatus()))
                    .toList();
            
            // Lấy số thành viên và trạng thái membership
            int memberCount = projectMemberService.getMembersOfProject(id).size();
            boolean isMember = projectService.isProjectMember(id, userId);
            boolean isOwner = project.getOwnerId() != null && project.getOwnerId().equals(userId);

                // Đổ dữ liệu vào Model
                model.addAttribute("project", project);
                model.addAttribute("todoTasks", todoTasks);
                model.addAttribute("doingTasks", doingTasks);
                model.addAttribute("doneTasks", doneTasks);
                model.addAttribute("memberCount", memberCount);
                model.addAttribute("isMember", isMember);
                model.addAttribute("isOwner", isOwner);
                model.addAttribute("taskStats", new TaskStats(
                    allTasks.size(),
                    doingTasks.size(),
                    doneTasks.size()
                ));


                return "project/detail";  // Kanban board view
            
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi khi tải dự án: " + e.getMessage());
            return "redirect:/projects";
        }
    }

    /**
     * GET /projects/{id}/edit - Trả về form chỉnh sửa dự án
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}/edit")
    public String editProjectForm(
            @PathVariable Long id,
            Model model,
            Authentication authentication) {
        return "redirect:/projects/" + id;
        /*
        try {
            Project project = projectService.getProjectById(id);
            if (project == null) {
                return "redirect:/projects";
            }
            
            model.addAttribute("project", project);
            model.addAttribute("isEditMode", true);
            return "project/form";
            
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/projects";
        }
        */
    }

    /**
     * POST /projects/{id}/update - Cập nhật thông tin dự án
     * Form submission từ HTML
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/update")
    public String updateProject(
            @PathVariable Long id,
            @ModelAttribute Project project,
            Authentication authentication,
            Model model) {
        return "redirect:/projects/" + id;
    }

    /**
     * POST /projects/{id}/join - User tham gia dự án
     */
    @PostMapping("/{id}/join")
    public String joinProject(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            Long userId = extractUserId(authentication);
            if (userId == null) return "redirect:/login";

            Project project = projectService.getProjectById(id);
            if ("PRIVATE".equalsIgnoreCase(project.getType())) {
                return "redirect:/projects?error=Project PRIVATE chỉ có thể tham gia khi được ADMIN mời";
            }

            projectService.addUserToProject(id, userId);
            return "redirect:/projects/" + id;
        } catch (Exception e) {
            return "redirect:/projects?error=" + e.getMessage();
        }
    }

    /**
     * POST /projects/{id}/leave - User rời dự án
     */
    @PostMapping("/{id}/leave")
    public String leaveProject(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            Long userId = extractUserId(authentication);
            if (userId == null) return "redirect:/login";
            
            projectMemberService.removeMemberFromProject(id, userId);
            return "redirect:/projects";
        } catch (Exception e) {
            return "redirect:/projects/" + id;
        }
    }

    /**
     * GET /projects/{id}/delete - Xóa dự án
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}/delete")
    public String deleteProject(@PathVariable Long id, Authentication authentication) {
        try {
            projectService.deleteProject(id);
            return "redirect:/projects";
        } catch (Exception e) {
            return "redirect:/projects/" + id;
        }
    }

    /**
     * Helper class để truyền stats sang template
     */
    public static class TaskStats {
        public int total;
        public int inProgress;
        public int completed;

        public TaskStats(int total, int inProgress, int completed) {
            this.total = total;
            this.inProgress = inProgress;
            this.completed = completed;
        }

        public int getTotal() { return total; }
        public int getInProgress() { return inProgress; }
        public int getCompleted() { return completed; }
        public int getPercentage() {
            return total == 0 ? 0 : (completed * 100 / total);
        }
    }
}
