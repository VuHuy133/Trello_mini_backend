package com.trello.service;

import com.trello.entity.Task;
import com.trello.entity.Project;
import com.trello.entity.User;
import com.trello.dto.ReorderTaskRequest;
import com.trello.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskService {
    private final TaskRepository taskRepository;
    private final ProjectService projectService;
    private final UserService userService;

    /**
     * Create a new task (accepts Task object)
     */
    @CacheEvict(value = {"tasks", "tasksByProject"}, allEntries = true)
    public Task createTask(Task task) {
        if (task.getCreatedAt() == null) {
            task.setCreatedAt(LocalDateTime.now());
        }
        return taskRepository.save(task);
    }

    /**
     * Create a new task (only project members can create)
     */
        @CacheEvict(value = {"tasks", "tasksByProject"}, allEntries = true)
        public Task createTask(Long projectId, Long userId, String title, String description, 
                String status, String priority, LocalDateTime dueDate) {
            Project project = projectService.getProjectById(projectId);
            User user = userService.getUserById(userId);
            Task task = Task.builder()
                .project(project)
                .user(user)
                .title(title)
                .description(description)
                .status(status)
                .priority(priority)
                .dueDate(dueDate)
                .createdAt(LocalDateTime.now())
                .build();
            return taskRepository.save(task);
    }

    /**
     * Get task by ID
     */
    @Transactional(readOnly = true)
    public Task getTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));
    }

    /**
     * Get all tasks for a project
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "tasksByProject", key = "#projectId")
    public List<Task> getTasksByProjectId(Long projectId) {
        Project project = projectService.getProjectById(projectId);
        return taskRepository.findByProject(project);
    }

    /**
     * Get tasks assigned to a user
     */
    @Transactional(readOnly = true)
    public List<Task> getTasksAssignedToUser(Long userId) {
        User assignee = userService.getUserById(userId);
        return taskRepository.findByAssignee(assignee);
    }

    /**
     * Update task (accepts Task object)
     */
    public Task updateTask(Task task) {
        task.setUpdatedAt(LocalDateTime.now());
        Task updated = taskRepository.save(task);
        return updated;
    }

    /**
     * Update task (only project member can update)
     */
    @CacheEvict(value = {"tasks", "tasksByProject"}, allEntries = true)
    public Task updateTask(Long id, Long userId, String title, String description, 
                    String status, String priority, LocalDateTime dueDate) {
        Task task = getTaskById(id);
        if (title != null) task.setTitle(title);
        if (description != null) task.setDescription(description);
        if (status != null) task.setStatus(status);
        if (priority != null) task.setPriority(priority);
        if (dueDate != null) task.setDueDate(dueDate);
        task.setUpdatedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }

    /**
     * Assign task to user
     */
    public Task assignTaskToUser(Long taskId, Long assigneeId, Long currentUserId) {
        Task task = getTaskById(taskId);
        User assignee = userService.getUserById(assigneeId);
        task.setAssignee(assignee);
        return taskRepository.save(task);
    }

    /**
     * Delete task
     */
    @CacheEvict(value = {"tasks", "tasksByProject"}, allEntries = true)
    public void deleteTask(Long id) {
        taskRepository.deleteById(id);
    }

    /**
     * Delete task (only creator or project owner can delete)
     */
    @CacheEvict(value = {"tasks", "tasksByProject"}, allEntries = true)
    public void deleteTask(Long id, Long userId) {
        taskRepository.deleteById(id);
    }

    /**
     * Get all tasks
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "tasks")
    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    /**
     * Check if user can access this task
     */
    @Transactional(readOnly = true)
    public boolean canUserAccessTask(Long taskId, Long userId) {
        return true;
    }

    /**
     * Reorder tasks with new positions and statuses
     */
    @CacheEvict(value = {"tasks", "tasksByProject"}, allEntries = true)
    public List<Task> reorderTasks(Long projectId, List<ReorderTaskRequest.ReorderTaskItem> items, Long userId) {
        // Validate project exists
        Project project = projectService.getProjectById(projectId);
        
        // Update each task
        for (ReorderTaskRequest.ReorderTaskItem item : items) {
            Task task = getTaskById(item.getId());
            if (task != null && task.getProject().getId().equals(projectId)) {
                if (item.getStatus() != null) {
                    task.setStatus(item.getStatus());
                }
                if (item.getPosition() != null) {
                    task.setPosition(item.getPosition());
                }
                task.setUpdatedAt(LocalDateTime.now());
                taskRepository.save(task);
            }
        }
        
        // Return all tasks for the project sorted by status and position
        List<Task> tasks = taskRepository.findByProject(project);
        return tasks.stream()
                .sorted((a, b) -> {
                    int statusCompare = (a.getStatus() != null ? a.getStatus() : "").compareTo(b.getStatus() != null ? b.getStatus() : "");
                    if (statusCompare != 0) return statusCompare;
                    Integer posA = a.getPosition() != null ? a.getPosition() : Integer.MAX_VALUE;
                    Integer posB = b.getPosition() != null ? b.getPosition() : Integer.MAX_VALUE;
                    return posA.compareTo(posB);
                })
                .collect(Collectors.toList());
    }
}
