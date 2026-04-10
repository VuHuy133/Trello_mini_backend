package com.trello.repository;

import com.trello.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import com.trello.entity.Project;
import com.trello.entity.User;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByProject(Project project);
    List<Task> findByAssignee(User assignee);
    List<Task> findByUser(User user);
    List<Task> findByProjectAndStatus(Project project, String status);
    // Custom query
    List<Task> findAllByProject(Project project);
}
