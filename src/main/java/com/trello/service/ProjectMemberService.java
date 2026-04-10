package com.trello.service;

import com.trello.entity.ProjectMember;
import com.trello.entity.Project;
import com.trello.entity.User;
import com.trello.repository.ProjectMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectMemberService {
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectService projectService;
    private final UserService userService;

    /**
     * Add member to project
     */
    public ProjectMember addMemberToProject(Long projectId, Long userId, String role) {
        Project project = projectService.getProjectById(projectId);
        User user = userService.getUserById(userId);
        ProjectMember member = ProjectMember.builder()
                .project(project)
                .user(user)
                .role(role)
                .build();
        return projectMemberRepository.save(member);
    }

    /**
     * Remove member from project
     */
    public void removeMemberFromProject(Long projectId, Long userId) {
        Project project = projectService.getProjectById(projectId);
        User user = userService.getUserById(userId);
        projectMemberRepository.deleteByProjectAndUser(project, user);
    }

    /**
     * Get all members of a project
     */
    @Transactional(readOnly = true)
    public List<ProjectMember> getMembersOfProject(Long projectId) {
        Project project = projectService.getProjectById(projectId);
        return projectMemberRepository.findByProject(project);
    }

    /**
     * Get member info
     */
    @Transactional(readOnly = true)
    public Optional<ProjectMember> getMemberInfo(Long projectId, Long userId) {
        Project project = projectService.getProjectById(projectId);
        User user = userService.getUserById(userId);
        return projectMemberRepository.findByProjectAndUser(project, user);
    }

    /**
     * Check if user is member of project
     */
    @Transactional(readOnly = true)
    public boolean isMemberOfProject(Long projectId, Long userId) {
        Project project = projectService.getProjectById(projectId);
        User user = userService.getUserById(userId);
        return projectMemberRepository.existsByProjectAndUser(project, user);
    }
}
