package com.trello.config.security;

import com.trello.entity.Task;
import com.trello.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class TaskSecurity {
    @Autowired
    private TaskService taskService;

    public boolean canEditTask(Authentication authentication, Long taskId) {
        Long userId = Long.parseLong(authentication.getName());
        Task task = taskService.getTaskById(taskId);
         return (task.getUser() != null && task.getUser().getId().equals(userId)) ||
             (task.getAssignee() != null && task.getAssignee().getId().equals(userId));
    }
}
