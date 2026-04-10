package com.trello.controller;

import com.trello.dto.ApiResponse;
import com.trello.dto.ProjectRequest;
import com.trello.dto.ProjectResponse;
import com.trello.dto.ProjectMemberResponse;
import com.trello.entity.Project;
import com.trello.entity.ProjectMember;
import com.trello.entity.User;
import com.trello.service.ProjectService;
import com.trello.service.ProjectMemberService;
import com.trello.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectMemberService projectMemberService;
    private final UserService userService;

    /**
     * Helper method: Extract userId from authentication
     * Handles both regular login (userId) and OAuth2 login (email)
     */
    private Long extractUserId(Authentication authentication) {
        if (authentication == null) return null;
        
        // Try to parse as Long (regular login)
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            // OAuth2 login - try to get email from OAuth2User
            try {
                Object principal = authentication.getPrincipal();
                if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User) {
                    org.springframework.security.oauth2.core.user.OAuth2User oauth2User = 
                        (org.springframework.security.oauth2.core.user.OAuth2User) principal;
                    String email = oauth2User.getAttribute("email");
                    if (email != null) {
                        User user = userService.findUserByEmail(email);
                        if (user != null) {
                            return user.getId();
                        }
                    }
                }
            } catch (Exception ex) {
                System.out.println("Error extracting userId from OAuth2: " + ex.getMessage());
            }
        }
        return null;
    }

    /**
     * GET /api/projects - Lấy danh sách dự án của User hiện tại
     */
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getUserProjects(Authentication authentication) {
        try {
            Long userId = extractUserId(authentication);
            if (userId == null) {
                return new ResponseEntity<>(ApiResponse.builder()
                        .success(false)
                        .message("Vui lòng đăng nhập")
                        .statusCode(HttpStatus.UNAUTHORIZED.value())
                        .build(), HttpStatus.UNAUTHORIZED);
            }
            
            boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equalsIgnoreCase(a.getAuthority()) || "ADMIN".equalsIgnoreCase(a.getAuthority()));

            List<Project> projects;
            if (isAdmin) {
                projects = projectService.getAllProjects();
            } else {
                projects = projectService.getProjectsByUserId(userId);
            }
            List<ProjectResponse> projectResponses = projects.stream()
                    .map(p -> convertToResponse(p, userId))
                    .collect(Collectors.toList());

            ApiResponse<?> response = ApiResponse.builder()
                    .success(true)
                    .message("Lấy danh sách dự án thành công")
                    .statusCode(HttpStatus.OK.value())
                    .data(projectResponses)
                    .build();

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            ApiResponse<?> response = ApiResponse.builder()
                    .success(false)
                    .message("Lỗi khi lấy danh sách dự án: " + e.getMessage())
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * POST /api/projects - Tạo dự án mới (Admin)
     */
    @PostMapping
    @PreAuthorize("has Role('ADMIN')")
    public ResponseEntity<ApiResponse<?>> createProject(
            @Valid @RequestBody ProjectRequest request,
            Authentication authentication) {
        try {
			Long userId = extractUserId(authentication);
			if (userId == null) {
                return new ResponseEntity<>(ApiResponse.builder()
                        .success(false)
                        .message("Vui lòng đăng nhập")
                        .statusCode(HttpStatus.UNAUTHORIZED.value())
                        .build(), HttpStatus.UNAUTHORIZED);
            }
			
			Project project = Project.builder()
				.name(request.getName())
				.description(request.getDescription())
				.type(request.getType())
				.build();

                Project createdProject = projectService.createProject(project);
                ProjectResponse projectResponse = convertToResponse(createdProject, userId);

            ApiResponse<?> response = ApiResponse.builder()
                    .success(true)
                    .message("Tạo dự án thành công")
                    .statusCode(HttpStatus.CREATED.value())
                    .data(projectResponse)
                    .build();

            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            ApiResponse<?> response = ApiResponse.builder()
                    .success(false)
                    .message("Lỗi khi tạo dự án: " + e.getMessage())
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * POST /api/projects/{id}/join - Tham gia vào dự án
     */
    @PostMapping("/{id}/join")
    public ResponseEntity<ApiResponse<?>> joinProject(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            Long userId = extractUserId(authentication);
            if (userId == null) {
                return new ResponseEntity<>(ApiResponse.builder()
                        .success(false)
                        .message("Vui lòng đăng nhập")
                        .statusCode(HttpStatus.UNAUTHORIZED.value())
                        .build(), HttpStatus.UNAUTHORIZED);
            }
            
            projectService.addUserToProject(id, userId);

            ApiResponse<?> response = ApiResponse.builder()
                    .success(true)
                    .message("Tham gia dự án thành công")
                    .statusCode(HttpStatus.OK.value())
                    .build();

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            ApiResponse<?> response = ApiResponse.builder()
                    .success(false)
                    .message("Lỗi khi tham gia dự án: " + e.getMessage())
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * DELETE /api/projects/{id} - Xóa dự án (Admin)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> deleteProject(@PathVariable Long id) {
        try {
            projectService.deleteProject(id);

            ApiResponse<?> response = ApiResponse.builder()
                    .success(true)
                    .message("Xóa dự án thành công")
                    .statusCode(HttpStatus.OK.value())
                    .build();

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            ApiResponse<?> response = ApiResponse.builder()
                    .success(false)
                    .message("Lỗi khi xóa dự án: " + e.getMessage())
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private ProjectMemberResponse mapProjectMemberToResponse(ProjectMember projectMember) {
        return ProjectMemberResponse.builder()
                .id(projectMember.getId())
                .projectId(projectMember.getProject().getId())
                .userId(projectMember.getUser().getId())
                .username(projectMember.getUser().getUsername())
                .email(projectMember.getUser().getEmail())
                .role(projectMember.getRole())
                .joinedAt(projectMember.getJoinedAt())
                .build();
    }

    // Overload: convertToResponse without userId (for admin context)
    private ProjectResponse convertToResponse(Project project) {
        return convertToResponse(project, null);
    }

    // Overload: convertToResponse with userId (to check if current user joined)
    private ProjectResponse convertToResponse(Project project, Long userId) {
        List<ProjectMember> members = projectMemberService.getMembersOfProject(project.getId());
        List<ProjectMemberResponse> memberResponses = members.stream()
                .map(this::mapProjectMemberToResponse)
                .collect(Collectors.toList());

        boolean joinedByCurrentUser = userId != null && projectService.isProjectMember(project.getId(), userId);

        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .ownerId(project.getOwnerId())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .type(project.getType())
                .memberCount(members.size())
                .taskCount((int) project.getTasks().size())
                .members(memberResponses)
                .joinedByCurrentUser(joinedByCurrentUser)
                .build();
    }
}
