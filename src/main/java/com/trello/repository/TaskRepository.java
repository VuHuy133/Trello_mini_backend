package com.trello.repository;

import com.trello.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import com.trello.entity.Project;
import com.trello.entity.User;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByProject(Project project);
    List<Task> findByAssignee(User assignee);
    List<Task> findByUser(User user);
    List<Task> findByProjectAndStatus(Project project, String status);
    List<Task> findAllByProject(Project project);
    
    // Pagination support
    Page<Task> findByProject(Project project, Pageable pageable);
    Page<Task> findByAssignee(User assignee, Pageable pageable);
    Page<Task> findByProjectAndStatus(Project project, String status, Pageable pageable);
    
    // Count queries optimized for dashboard
    @Query("SELECT COUNT(t) FROM Task t WHERE t.status = :status")
    long countByStatus(@Param("status") String status);
    
    @Query("SELECT COUNT(t) FROM Task t WHERE t.status = :status AND t.project.id = :projectId")
    long countByStatusAndProjectId(@Param("status") String status, @Param("projectId") Long projectId);
    
    // Overdue tasks query optimized with index
    @Query("SELECT t FROM Task t WHERE t.dueDate < :now AND t.status != 'DONE' ORDER BY t.dueDate ASC")
    Page<Task> findOverdueTasks(@Param("now") LocalDateTime now, Pageable pageable);
    
    @Query("SELECT COUNT(t) FROM Task t WHERE t.dueDate < :now AND t.status != 'DONE'")
    long countOverdueTasks(@Param("now") LocalDateTime now);
    
    // Get tasks by status with pagination
    @Query("SELECT t FROM Task t WHERE t.status = :status ORDER BY t.createdAt DESC")
    Page<Task> findByStatusPaged(@Param("status") String status, Pageable pageable);
}
