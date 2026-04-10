package com.trello.repository;

import com.trello.entity.TaskAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskAttachmentRepository extends JpaRepository<TaskAttachment, Long> {
    List<TaskAttachment> findByTaskId(Long taskId);
    List<TaskAttachment> findByTaskIdOrderByCreatedAtDesc(Long taskId);
    // Custom query
    List<TaskAttachment> findAllByTaskId(Long taskId);
}
