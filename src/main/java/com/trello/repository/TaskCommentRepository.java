package com.trello.repository;

import com.trello.entity.TaskComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {
    List<TaskComment> findByTaskId(Long taskId);
    List<TaskComment> findByTaskIdOrderByCreatedAtDesc(Long taskId);
    // Custom query
    List<TaskComment> findAllByTaskId(Long taskId);
}
