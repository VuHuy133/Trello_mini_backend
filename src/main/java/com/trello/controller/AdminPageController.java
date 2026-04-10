package com.trello.controller;

import com.trello.entity.Project;
import com.trello.entity.Task;
import com.trello.entity.User;
import com.trello.service.ProjectMemberService;
import com.trello.service.ProjectService;
import com.trello.service.TaskService;
import com.trello.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminPageController {

    private final UserService userService;
    private final TaskService taskService;
    private final ProjectService projectService;
    private final ProjectMemberService projectMemberService;

    @ModelAttribute
    public void ensureSessionUser(HttpSession session) {
        try {
            if (session.getAttribute("user") == null) {
                return;
            }
            User sessionUser = (User) session.getAttribute("user");
            if (sessionUser.getId() != null) {
                User freshUser = userService.getUserById(sessionUser.getId());
                session.setAttribute("user", freshUser);
            }
        } catch (Exception e) {
            // ignore — session user stays stale, do not crash request
        }
    }

    /**
     * GET /admin - Dashboard thống kê
     */
    @GetMapping
    public String dashboard(Model model) {
        try {
            List<User> users = userService.getAllUsers();
            List<Project> projects = projectService.getAllProjects();
            List<Task> tasks = taskService.getAllTasks();

            long adminCount = users.stream().filter(u -> "ADMIN".equals(u.getRole())).count();
            long todoCount = tasks.stream().filter(t -> "TODO".equals(t.getStatus())).count();
            long doingCount = tasks.stream().filter(t -> "DOING".equals(t.getStatus())).count();
            long doneCount = tasks.stream().filter(t -> "DONE".equals(t.getStatus())).count();

            model.addAttribute("totalUsers", users.size());
            model.addAttribute("totalProjects", projects.size());
            model.addAttribute("totalTasks", tasks.size());
            model.addAttribute("adminCount", adminCount);
            model.addAttribute("todoCount", todoCount);
            model.addAttribute("doingCount", doingCount);
            model.addAttribute("doneCount", doneCount);

            return "admin/dashboard";
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi khi tải dữ liệu dashboard: " + e.getMessage());
            model.addAttribute("totalUsers", 0);
            model.addAttribute("totalProjects", 0);
            model.addAttribute("totalTasks", 0);
            return "admin/dashboard";
        }
    }

    /**
     * GET /admin/users - Quản lý users
     */
    @GetMapping("/users")
    public String userList(Model model) {
        List<User> users = userService.getAllUsers();
        model.addAttribute("users", users);
        return "admin/users";
    }

    /**
     * POST /admin/users/{id}/role - Toggle role USER <-> ADMIN
     */
    @PostMapping("/users/{id}/role")
    public String toggleRole(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser.getId().equals(id)) {
            ra.addFlashAttribute("error", "Không thể thay đổi role của chính mình!");
            return "redirect:/admin/users";
        }

        User user = userService.getUserById(id);
        String newRole = "ADMIN".equals(user.getRole()) ? "USER" : "ADMIN";
        userService.updateRole(id, newRole);
        ra.addFlashAttribute("success", "Đã đổi role của " + user.getEmail() + " thành " + newRole);
        return "redirect:/admin/users";
    }

    /**
     * POST /admin/users/{id}/delete - Xóa user
     */
    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser.getId().equals(id)) {
            ra.addFlashAttribute("error", "Không thể xóa chính mình!");
            return "redirect:/admin/users";
        }

        User user = userService.getUserById(id);
        userService.deleteUser(id);
        ra.addFlashAttribute("success", "Đã xóa user " + user.getEmail());
        return "redirect:/admin/users";
    }

    /**
     * GET /admin/tasks - Tất cả tasks
     */
    @GetMapping("/tasks")
    public String taskList(Model model) {
        try {
            List<Task> tasks = taskService.getAllTasks();
            List<Project> projects = projectService.getAllProjects();

            Map<Long, String> projectNames = new HashMap<>();
            for (Project p : projects) {
                projectNames.put(p.getId(), p.getName());
            }

            Map<Long, String> assigneeNames = new HashMap<>();
            for (Task t : tasks) {
                Long assigneeId = (t.getAssignee() != null) ? t.getAssignee().getId() : null;
                if (assigneeId != null && !assigneeNames.containsKey(assigneeId)) {
                    try {
                        User u = userService.getUserById(assigneeId);
                        assigneeNames.put(assigneeId, u.getFullName());
                    } catch (Exception e) {
                        assigneeNames.put(assigneeId, "User #" + assigneeId);
                    }
                }
            }

            model.addAttribute("tasks", tasks);
            model.addAttribute("projectNames", projectNames);
            model.addAttribute("assigneeNames", assigneeNames);
            return "admin/tasks";
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi khi tải dữ liệu tasks: " + e.getMessage());
            model.addAttribute("tasks", new java.util.ArrayList<>());
            model.addAttribute("projectNames", new java.util.HashMap<>());
            model.addAttribute("assigneeNames", new java.util.HashMap<>());
            return "admin/tasks";
        }
    }

    /**
     * POST /admin/tasks/{id}/delete - Xóa task
     */
    @PostMapping("/tasks/{id}/delete")
    public String deleteTask(@PathVariable Long id, RedirectAttributes ra) {
        Task task = taskService.getTaskById(id);
        taskService.deleteTask(id);
        ra.addFlashAttribute("success", "Đã xóa task: " + task.getTitle());
        return "redirect:/admin/tasks";
    }

    /**
     * GET /admin/projects - Tất cả projects
     */
    @GetMapping("/projects")
    public String projectList(Model model, HttpSession session) {
        try {
            List<Project> projects = projectService.getAllProjects();
            List<User> users = userService.getAllUsers();

            Map<Long, Integer> memberCounts = new HashMap<>();
            Map<Long, Integer> taskCounts = new HashMap<>();
            Map<Long, String> ownerNames = new HashMap<>();
            Map<Long, Boolean> isMember = new HashMap<>();
            
            // Lấy sessionUser để kiểm tra xem admin đã tham gia project nào
            User sessionUser = (User) session.getAttribute("user");
            Long currentUserId = sessionUser != null ? sessionUser.getId() : null;

            for (Project p : projects) {
                try {
                    memberCounts.put(p.getId(), projectMemberService.getMembersOfProject(p.getId()).size());
                    taskCounts.put(p.getId(), taskService.getTasksByProjectId(p.getId()).size());
                    if (currentUserId != null) {
                        isMember.put(p.getId(), projectService.isProjectMember(p.getId(), currentUserId));
                    } else {
                        isMember.put(p.getId(), false);
                    }
                } catch (Exception e) {
                    memberCounts.put(p.getId(), 0);
                    taskCounts.put(p.getId(), 0);
                    isMember.put(p.getId(), false);
                }
                if (p.getOwnerId() != null && !ownerNames.containsKey(p.getOwnerId())) {
                    try {
                        User owner = userService.getUserById(p.getOwnerId());
                        ownerNames.put(p.getOwnerId(), owner.getFullName());
                    } catch (Exception e) {
                        ownerNames.put(p.getOwnerId(), "User #" + p.getOwnerId());
                    }
                }
            }

            model.addAttribute("projects", projects);
            model.addAttribute("memberCounts", memberCounts);
            model.addAttribute("taskCounts", taskCounts);
            model.addAttribute("ownerNames", ownerNames);
            model.addAttribute("users", users);
            model.addAttribute("isMember", isMember);
            model.addAttribute("currentUserId", currentUserId);
            return "admin/projects";
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi khi tải dữ liệu projects: " + e.getMessage());
            model.addAttribute("projects", new java.util.ArrayList<>());
            model.addAttribute("memberCounts", new java.util.HashMap<>());
            model.addAttribute("taskCounts", new java.util.HashMap<>());
            model.addAttribute("ownerNames", new java.util.HashMap<>());
            model.addAttribute("users", new java.util.ArrayList<>());
            model.addAttribute("isMember", new java.util.HashMap<>());
            model.addAttribute("currentUserId", null);
            return "admin/projects";
        }
    }

    /**
     * POST /admin/projects/{projectId}/add-members - Thêm thành viên vào project (ADMIN)
     */
    @PostMapping("/projects/{projectId}/add-members")
    public String addMemberToProject(
            @PathVariable Long projectId,
            @RequestParam(name = "userId", required = false) java.util.List<Long> userIds,
            RedirectAttributes ra) {
        if (userIds == null || userIds.isEmpty()) {
            ra.addFlashAttribute("error", "Chưa chọn thành viên để thêm.");
            return "redirect:/admin/projects";
        }
        int added = 0;
        for (Long userId : userIds) {
            try {
                projectService.addUserToProject(projectId, userId);
                added++;
            } catch (Exception e) {
                // ignore individual failures (e.g., already member) and continue
            }
        }
        ra.addFlashAttribute("success", "Đã thêm " + added + " thành viên vào project #" + projectId);
        return "redirect:/admin/projects";
    }

    /**
     * POST /admin/projects/{id}/delete - Xóa project
     */
    @PostMapping("/projects/{id}/delete")
    public String deleteProject(@PathVariable Long id, RedirectAttributes ra) {
        Project project = projectService.getProjectById(id);
        projectService.deleteProject(id);
        ra.addFlashAttribute("success", "Đã xóa project: " + project.getName());
        return "redirect:/admin/projects";
    }
}
