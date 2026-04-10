package com.trello.repository;

import com.trello.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import com.trello.entity.Project;
import com.trello.entity.User;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
    Optional<ProjectMember> findByProjectAndUser(Project project, User user);
    boolean existsByProjectAndUser(Project project, User user);
    List<ProjectMember> findByProject(Project project);
    List<ProjectMember> findByUser(User user);
    void deleteByProjectAndUser(Project project, User user);
    // Custom query
    List<ProjectMember> findAllByProject(Project project);
    List<ProjectMember> findAllByUser(User user);
}
