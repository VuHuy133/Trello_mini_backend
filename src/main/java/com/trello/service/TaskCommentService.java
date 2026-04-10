package com.trello.service;

import com.trello.entity.TaskComment;
import com.trello.repository.TaskCommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskCommentService {
    private final TaskCommentRepository taskCommentRepository;

    /**
     * Get all task comments
     */
    @Transactional(readOnly = true)
    public List<TaskComment> fetchTaskComments() {
        return this.taskCommentRepository.findAll();
    }

    /**
     * Create a new comment
     */
    public TaskComment createComment(TaskComment comment) {
        return taskCommentRepository.save(comment);
    }

    /**
     * Get comments by task ID
     */
    @Transactional(readOnly = true)
    public List<TaskComment> getCommentsByTaskId(Long taskId) {
        return taskCommentRepository.findAll()
            .stream()
            .filter(comment -> comment.getTask() != null && comment.getTask().getId().equals(taskId))
            .toList();
    }
}
