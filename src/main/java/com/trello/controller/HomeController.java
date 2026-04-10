package com.trello.controller;

import com.trello.entity.User;
import com.trello.entity.Project;
import com.trello.entity.Task;
import com.trello.service.UserService;
import com.trello.service.ProjectService;
import com.trello.service.ProjectMemberService;
import com.trello.service.TaskService;
import com.trello.exception.UserAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;

import java.util.List;

/**
 * HomeController - Xử lý trang chủ (Landing page + Dashboard)
 * 
 * Bước 1: GET / - Truyền dữ liệu từ Java sang HTML
 * Bước 2: Form submission (POST) - Nhận dữ liệu từ HTML
 */
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final UserService userService;
    private final ProjectService projectService;
    private final ProjectMemberService projectMemberService;
    private final TaskService taskService;

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

    // ========== GET MAPPINGS (Show Pages) ==========
    
    /**
     * Bước 1: GET / - Trang chủ (Landing page + Dashboard)
     * Model: Truyền projects, tasks, stats → HTML
     */
    @GetMapping("/")
    public String home(Model model, Authentication authentication, HttpSession session) {
        try {
            // Kiểm tra nếu user chưa đăng nhập
            if (authentication == null || !authentication.isAuthenticated()) {
                // Landing page view - không cần dữ liệu
                return "index";  // Thymeleaf sẽ show landing page
            }
            
            // User đã đăng nhập - trả về dashboard
            Long userId = extractUserId(authentication);
            if (userId == null) {
                return "redirect:/login";
            }
            
            User user = userService.getUserById(userId);
            System.out.println("DEBUG HomeController: userId=" + userId + ", user=" + user + ", role=" + (user != null ? user.getRole() : "NULL"));
            model.addAttribute("user", user);  // Thêm user vào model để Thymeleaf truy cập
            session.setAttribute("user", user);  // Thêm user vào session
            
            // Lấy danh sách projects
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equalsIgnoreCase(a.getAuthority()) || "ADMIN".equalsIgnoreCase(a.getAuthority()));
            List<Project> projects = isAdmin ? projectService.getAllProjects() : projectService.getProjectsByUserId(userId);
            
            // Tính task count và done count cho từng project
            java.util.Map<Long, Integer> taskCounts = new java.util.HashMap<>();
            java.util.Map<Long, Integer> doneCounts = new java.util.HashMap<>();
            java.util.Map<Long, String> projectNames = new java.util.HashMap<>();
            java.util.Map<Long, Boolean> isMember = new java.util.HashMap<>();
            for (Project p : projects) {
                List<Task> projectTasks = taskService.getTasksByProjectId(p.getId());
                taskCounts.put(p.getId(), projectTasks.size());
                doneCounts.put(p.getId(), (int) projectTasks.stream()
                        .filter(t -> "DONE".equalsIgnoreCase(t.getStatus())).count());
                projectNames.put(p.getId(), p.getName());
                isMember.put(p.getId(), projectService.isProjectMember(p.getId(), userId));
            }
            
            // Lấy danh sách tasks để tính statistics
            // Admin: tất cả tasks trong hệ thống, User: chỉ tasks giao cho họ
            List<Task> taskListForStats;
            if (isAdmin) {
                taskListForStats = taskService.getAllTasks();
            } else {
                taskListForStats = taskService.getTasksAssignedToUser(userId);
            }
            
            // Tính toán statistics
            long totalTasks = taskListForStats.size();
            long completedTasks = taskListForStats.stream()
                    .filter(t -> "DONE".equalsIgnoreCase(t.getStatus()))
                    .count();
            long inProgressTasks = taskListForStats.stream()
                    .filter(t -> "DOING".equalsIgnoreCase(t.getStatus()))
                    .count();
            long overdueTasks = taskListForStats.stream()
                    .filter(t -> t.getDueDate() != null && 
                               t.getDueDate().isBefore(java.time.LocalDateTime.now()) &&
                               !"DONE".equalsIgnoreCase(t.getStatus()))
                    .count();
            
            // Lấy tasks giao cho user để hiển thị trong danh sách
            List<Task> tasks = taskService.getTasksAssignedToUser(userId);
            
            // Build assigneeNames map
            java.util.Map<Long, String> assigneeNames = new java.util.HashMap<>();
            for (Task task : taskListForStats) {
                if (task.getAssignee() != null) {
                    assigneeNames.put(task.getAssignee().getId(), task.getAssignee().getFullName() != null ? task.getAssignee().getFullName() : task.getAssignee().getUsername());
                }
            }
            
            // Bước 2: Đổ dữ liệu vào Model
            // index.html sẽ truy cập qua ${projects}, ${tasks}, ${stats}
            model.addAttribute("projects", projects);
            model.addAttribute("taskCounts", taskCounts);
            model.addAttribute("doneCounts", doneCounts);
            model.addAttribute("projectNames", projectNames);
            model.addAttribute("isMember", isMember);
            model.addAttribute("tasks", tasks);
            model.addAttribute("assigneeNames", assigneeNames);
            model.addAttribute("stats", new DashboardStats(
                    totalTasks,
                    completedTasks,
                    inProgressTasks,
                    overdueTasks,
                    projects.size()
            ));
            
            // Dữ liệu admin dashboard
            if (isAdmin) {
                long adminTotalUsers = userService.getAllUsers().size();
                long adminTotalProjects = projectService.getAllProjects().size();
                List<Task> allTasks = taskService.getAllTasks();
                long adminTotalTasks = allTasks.size();
                long adminCount = userService.getAllUsers().stream()
                        .filter(u -> "ADMIN".equalsIgnoreCase(u.getRole()))
                        .count();
                long doingCount = allTasks.stream()
                        .filter(t -> "DOING".equalsIgnoreCase(t.getStatus()))
                        .count();
                
                model.addAttribute("totalUsers", adminTotalUsers);
                model.addAttribute("totalProjects", adminTotalProjects);
                model.addAttribute("totalTasks", adminTotalTasks);
                model.addAttribute("adminCount", adminCount);
                model.addAttribute("doingCount", doingCount);
            }
            
            model.addAttribute("currentPath", "/");
            return "index";  // Thymeleaf sẽ show dashboard view
        
        } catch (Exception e) {
            System.out.println("ERROR in HomeController.home(): " + e.getMessage());
            e.printStackTrace();
            
            // Thêm user mặc định vào session nếu có lỗi
            User emptyUser = new User();
            model.addAttribute("user", emptyUser);
            session.setAttribute("user", emptyUser);
            
            model.addAttribute("error", "Lỗi khi tải dữ liệu: " + e.getMessage());
            model.addAttribute("currentPath", "/");
            return "index";
        }
    }

    /**
     * GET /user - Trang dashboard cho USER role
     */
    @GetMapping("/user")
    public String userDashboard(Model model, Authentication authentication, HttpSession session) {
        try {
            // Kiểm tra nếu user chưa đăng nhập
            if (authentication == null || !authentication.isAuthenticated()) {
                return "redirect:/login";
            }
            
            // User đã đăng nhập - trả về user dashboard
            Long userId = extractUserId(authentication);
            if (userId == null) {
                return "redirect:/login";
            }
            
            // Lấy user từ database, với proper error handling
            User user = null;
            try {
                user = userService.getUserById(userId);
            } catch (Exception e) {
                System.out.println("ERROR: User not found with id: " + userId + " - " + e.getMessage());
                // Tạo user mặc định nếu không tìm thấy
                user = new User();
                user.setId(userId);
                user.setRole("USER");
            }
            
            // Ensure user role is set (default to USER if null)
            if (user != null && user.getRole() == null) {
                user.setRole("USER");
            }
            
            // Nếu là ADMIN, redirect tới /admin
            if (user != null && user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole())) {
                return "redirect:/admin";
            }
            
            model.addAttribute("user", user != null ? user : new User());
            session.setAttribute("user", user != null ? user : new User());  // Thêm vào session
            
            // Lấy danh sách projects của user
            List<Project> projects = null;
            try {
                projects = projectService.getProjectsByUserId(userId);
            } catch (Exception e) {
                System.out.println("ERROR getting projects: " + e.getMessage());
                projects = new java.util.ArrayList<>();
            }
            if (projects == null) {
                projects = new java.util.ArrayList<>();
            }
            
            // Lấy danh sách tất cả public projects
            List<Project> publicProjects = null;
            try {
                publicProjects = projectService.getAllPublicProjects();
            } catch (Exception e) {
                System.out.println("ERROR getting public projects: " + e.getMessage());
                publicProjects = new java.util.ArrayList<>();
            }
            if (publicProjects == null) {
                publicProjects = new java.util.ArrayList<>();
            }
            
            // Kết hợp: thêm các public projects mà user chưa tham gia vào danh sách projects
            java.util.Set<Long> userProjectIds = new java.util.HashSet<>();
            for (Project p : projects) {
                if (p != null) {
                    userProjectIds.add(p.getId());
                }
            }
            
            for (Project p : publicProjects) {
                if (p != null && !userProjectIds.contains(p.getId())) {
                    projects.add(p);
                }
            }
            
            // Tính task count và done count cho từng project
            java.util.Map<Long, Integer> taskCounts = new java.util.HashMap<>();
            java.util.Map<Long, Integer> doneCounts = new java.util.HashMap<>();
            java.util.Map<Long, String> projectNames = new java.util.HashMap<>();
            java.util.Map<Long, Boolean> isMember = new java.util.HashMap<>();
            
            for (Project p : projects) {
                if (p != null) {
                    try {
                        List<Task> projectTasks = taskService.getTasksByProjectId(p.getId());
                        if (projectTasks == null) projectTasks = new java.util.ArrayList<>();
                        taskCounts.put(p.getId(), projectTasks.size());
                        doneCounts.put(p.getId(), (int) projectTasks.stream()
                                .filter(t -> t != null && "DONE".equalsIgnoreCase(t.getStatus())).count());
                        projectNames.put(p.getId(), p.getName());
                        
                        try {
                            isMember.put(p.getId(), projectService.isProjectMember(p.getId(), userId));
                        } catch (Exception e) {
                            isMember.put(p.getId(), false);
                        }
                    } catch (Exception e) {
                        System.out.println("ERROR getting tasks for project " + p.getId() + ": " + e.getMessage());
                        taskCounts.put(p.getId(), 0);
                        doneCounts.put(p.getId(), 0);
                        projectNames.put(p.getId(), p.getName());
                        isMember.put(p.getId(), false);
                    }
                }
            }
            
            // Lấy danh sách tasks giao cho user
            List<Task> taskListForStats = null;
            try {
                taskListForStats = taskService.getTasksAssignedToUser(userId);
            } catch (Exception e) {
                System.out.println("ERROR getting tasks assigned to user: " + e.getMessage());
                taskListForStats = new java.util.ArrayList<>();
            }
            if (taskListForStats == null) {
                taskListForStats = new java.util.ArrayList<>();
            }
            
            // Tính toán statistics
            long totalTasks = taskListForStats.size();
            long completedTasks = taskListForStats.stream()
                    .filter(t -> t != null && "DONE".equalsIgnoreCase(t.getStatus()))
                    .count();
            long inProgressTasks = taskListForStats.stream()
                    .filter(t -> t != null && "DOING".equalsIgnoreCase(t.getStatus()))
                    .count();
            long overdueTasks = taskListForStats.stream()
                    .filter(t -> t != null && t.getDueDate() != null && 
                               t.getDueDate().isBefore(java.time.LocalDateTime.now()) &&
                               !"DONE".equalsIgnoreCase(t.getStatus()))
                    .count();
            
            // Lấy tasks giao cho user để hiển thị trong danh sách
            List<Task> tasks = null;
            try {
                tasks = taskService.getTasksAssignedToUser(userId);
            } catch (Exception e) {
                System.out.println("ERROR getting tasks list: " + e.getMessage());
                tasks = new java.util.ArrayList<>();
            }
            if (tasks == null) {
                tasks = new java.util.ArrayList<>();
            }
            
            // Build assigneeNames map
            java.util.Map<Long, String> assigneeNames = new java.util.HashMap<>();
            for (Task task : taskListForStats) {
                if (task != null && task.getAssignee() != null && task.getAssignee().getId() != null) {
                    assigneeNames.put(task.getAssignee().getId(), 
                        task.getAssignee().getFullName() != null ? task.getAssignee().getFullName() : task.getAssignee().getUsername());
                }
            }
            
            // Đổ dữ liệu vào Model
            model.addAttribute("projects", projects);
            model.addAttribute("taskCounts", taskCounts);
            model.addAttribute("doneCounts", doneCounts);
            model.addAttribute("projectNames", projectNames);
            model.addAttribute("isMember", isMember);
            model.addAttribute("tasks", tasks);
            model.addAttribute("assigneeNames", assigneeNames);
            model.addAttribute("stats", new DashboardStats(
                    totalTasks,
                    completedTasks,
                    inProgressTasks,
                    overdueTasks,
                    projects.size()
            ));
            
            model.addAttribute("currentPath", "/user");
            return "index";  // Sử dụng cùng template nhưng chỉ render user dashboard
            
        } catch (Exception e) {
            System.out.println("ERROR in HomeController.userDashboard(): " + e.getMessage());
            e.printStackTrace();
            
            // Khởi tạo dữ liệu mặc định khi có lỗi
            User emptyUser = new User();
            emptyUser.setRole("USER");
            model.addAttribute("user", emptyUser);
            session.setAttribute("user", emptyUser);  // Thêm vào session
            model.addAttribute("projects", new java.util.ArrayList<>());
            model.addAttribute("taskCounts", new java.util.HashMap<>());
            model.addAttribute("doneCounts", new java.util.HashMap<>());
            model.addAttribute("projectNames", new java.util.HashMap<>());
            model.addAttribute("isMember", new java.util.HashMap<>());
            model.addAttribute("tasks", new java.util.ArrayList<>());
            model.addAttribute("assigneeNames", new java.util.HashMap<>());
            model.addAttribute("stats", new DashboardStats(0, 0, 0, 0, 0));
            model.addAttribute("error", "Lỗi khi tải dữ liệu: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
            model.addAttribute("currentPath", "/user");
            
            return "index";
        }
    }

    /**
     * GET /home - Redirect to home
     */
    @GetMapping("/home")
    public String homeRedirect() {
        return "redirect:/";
    }

    /**
     * GET /login - Show login form
     */
    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    /**
     * GET /register - Show register form
     */
    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("user", new User());
        return "auth/register";
    }

    // ========== POST MAPPINGS (Form Submission) ==========
    
    /**
     * Bước 3: POST /register - Nhận dữ liệu từ HTML Form
     * @ModelAttribute: Spring tự động map form fields → User object
     */
    @PostMapping("/register")
    public String handleRegister(@ModelAttribute User user, Model model) {
        try {
            // Check if email already exists
            if (userService.isEmailExist(user.getEmail())) {
                model.addAttribute("error", "Email đã tồn tại. Vui lòng sử dụng email khác.");
                model.addAttribute("user", user);
                return "auth/register";
            }

            // Validate input
            if (user.getName() == null || user.getName().trim().isEmpty()) {
                model.addAttribute("error", "Tên không được để trống.");
                model.addAttribute("user", user);
                return "auth/register";
            }

            if (user.getPassword() == null || user.getPassword().length() < 6) {
                model.addAttribute("error", "Mật khẩu phải có ít nhất 6 ký tự.");
                model.addAttribute("user", user);
                return "auth/register";
            }

            // Set username from email if not provided (form doesn't have username field)
            if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
                user.setUsername(user.getEmail());
            }

            // Register user
            User registeredUser = userService.handleRegister(user);
            
            // Redirect to login with success message
            return "redirect:/login?success";

        } catch (UserAlreadyExistsException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("user", user);
            return "auth/register";
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi đăng ký: " + e.getMessage());
            model.addAttribute("user", user);
            return "auth/register";
        }
    }

    /**
     * POST /login - Handled by Spring Security Form Login
     * Note: Spring Security intercepts POST /login and handles authentication
     * FormLogin is configured in SecurityConfig.java
     */

    /**
     * Helper class để truyền dashboard statistics sang Thymeleaf template
     */
    public static class DashboardStats {
        public long totalTasks;
        public long completedTasks;
        public long inProgressTasks;
        public long overdueTasks;
        public long totalProjects;

        public DashboardStats(long totalTasks, long completedTasks, 
                            long inProgressTasks, long overdueTasks, long totalProjects) {
            this.totalTasks = totalTasks;
            this.completedTasks = completedTasks;
            this.inProgressTasks = inProgressTasks;
            this.overdueTasks = overdueTasks;
            this.totalProjects = totalProjects;
        }

        // Getters for Thymeleaf
        public long getTotalTasks() { return totalTasks; }
        public long getCompletedTasks() { return completedTasks; }
        public long getInProgressTasks() { return inProgressTasks; }
        public long getOverdueTasks() { return overdueTasks; }
        public long getTotalProjects() { return totalProjects; }
        
        public long getCompletionPercentage() {
            return totalTasks == 0 ? 0 : (completedTasks * 100 / totalTasks);
        }
    }
}
