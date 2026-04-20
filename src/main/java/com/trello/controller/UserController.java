package com.trello.controller;

import com.trello.dto.ApiResponse;
import com.trello.dto.UserRequest;
import com.trello.dto.UserResponse;
import com.trello.dto.ProjectMemberResponse;
import com.trello.entity.User;
import com.trello.entity.ProjectMember;
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
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null) return null;
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            try {
                Object principal = authentication.getPrincipal();
                if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User) {
                    org.springframework.security.oauth2.core.user.OAuth2User oauth2User = 
                        (org.springframework.security.oauth2.core.user.OAuth2User) principal;
                    String email = oauth2User.getAttribute("email");
                    if (email != null) {
                        User user = userService.findUserByEmail(email);
                        if (user != null) return user.getId();
                    }
                }
            } catch (Exception ex) {
                System.out.println("Error extracting userId from OAuth2: " + ex.getMessage());
            }
        }
        return null;
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
    
    private UserResponse mapToResponse(User user) {
        List<ProjectMember> joinedProjects = userService.getUserJoinedProjects(user.getId());
        List<ProjectMemberResponse> joinedProjectResponses = joinedProjects.stream()
                .map(this::mapProjectMemberToResponse)
                .collect(Collectors.toList());
        
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .joinedProjects(joinedProjectResponses)
                .build();
    }
    
    /**
     * Get user by ID
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<?>> getUserById(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        
        ApiResponse<?> response = ApiResponse.builder()
                .success(true)
                .message("User retrieved successfully")
                .statusCode(HttpStatus.OK.value())
                .data(mapToResponse(user))
                .build();
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    /**
     * Get current user profile
     */
    @GetMapping("/profile/me")
    public ResponseEntity<ApiResponse<?>> getCurrentUserProfile(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        if (userId == null) {
            ApiResponse<?> response = ApiResponse.builder()
                    .success(false)
                    .message("Vui lòng đăng nhập")
                    .statusCode(HttpStatus.UNAUTHORIZED.value())
                    .build();
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
        User user = userService.getUserById(userId);
        
        ApiResponse<?> response = ApiResponse.builder()
                .success(true)
                .message("User profile retrieved successfully")
                .statusCode(HttpStatus.OK.value())
                .data(mapToResponse(user))
                .build();
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    /**
     * Update user profile
     */
    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<?>> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UserRequest request,
            Authentication authentication) {
        
        Long currentUserId = getCurrentUserId(authentication);
        if (currentUserId == null) {
            ApiResponse<?> response = ApiResponse.builder()
                    .success(false)
                    .message("Vui lòng đăng nhập")
                    .statusCode(HttpStatus.UNAUTHORIZED.value())
                    .build();
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
        
        // Users can only update their own profile
        if (!userId.equals(currentUserId)) {
            ApiResponse<?> response = ApiResponse.builder()
                    .success(false)
                    .message("You can only update your own profile")
                    .statusCode(HttpStatus.FORBIDDEN.value())
                    .build();
            return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
        }
        
        User user = userService.updateUser(userId, request.getUsername(), request.getEmail());
        
        ApiResponse<?> response = ApiResponse.builder()
                .success(true)
                .message("User updated successfully")
                .statusCode(HttpStatus.OK.value())
                .data(mapToResponse(user))
                .build();
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    /**
     * Delete user account
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<?>> deleteUser(
            @PathVariable Long userId,
            Authentication authentication) {
        
        Long currentUserId = getCurrentUserId(authentication);
        if (currentUserId == null) {
            ApiResponse<?> response = ApiResponse.builder()
                    .success(false)
                    .message("Vui lòng đăng nhập")
                    .statusCode(HttpStatus.UNAUTHORIZED.value())
                    .build();
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
        
        // Users can only delete their own account
        if (!userId.equals(currentUserId)) {
            ApiResponse<?> response = ApiResponse.builder()
                    .success(false)
                    .message("You can only delete your own account")
                    .statusCode(HttpStatus.FORBIDDEN.value())
                    .build();
            return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
        }
        
        userService.deleteUser(userId);
        
        ApiResponse<?> response = ApiResponse.builder()
                .success(true)
                .message("User deleted successfully")
                .statusCode(HttpStatus.OK.value())
                .build();
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Change current user password
     */
    @PutMapping("/{userId}/password")
    public ResponseEntity<ApiResponse<?>> changePassword(
            @PathVariable Long userId,
            @RequestBody java.util.Map<String, String> body,
            Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        if (currentUserId == null || !userId.equals(currentUserId)) {
            ApiResponse<?> response = ApiResponse.builder()
                    .success(false).message("Không có quyền thực hiện thao tác này")
                    .statusCode(HttpStatus.FORBIDDEN.value()).build();
            return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
        }
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        if (oldPassword == null || newPassword == null) {
            ApiResponse<?> response = ApiResponse.builder()
                    .success(false).message("Thiếu mật khẩu cũ hoặc mới")
                    .statusCode(HttpStatus.BAD_REQUEST.value()).build();
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        User user = userService.getUserById(userId);
        if (!userService.validatePassword(oldPassword, user.getPassword())) {
            ApiResponse<?> response = ApiResponse.builder()
                    .success(false).message("Mật khẩu cũ không đúng")
                    .statusCode(HttpStatus.BAD_REQUEST.value()).build();
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        userService.updatePassword(userId, userService.encodePassword(newPassword));
        ApiResponse<?> response = ApiResponse.builder()
                .success(true).message("Đổi mật khẩu thành công")
                .statusCode(HttpStatus.OK.value()).build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * ADMIN: Get all users (ADMIN only)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        List<UserResponse> userResponses = users.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        
        ApiResponse<?> response = ApiResponse.builder()
                .success(true)
                .message("All users retrieved successfully")
                .statusCode(HttpStatus.OK.value())
                .data(userResponses)
                .build();
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * ADMIN: Create new user (ADMIN only)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> createUser(@Valid @RequestBody User newUser) {
        User createdUser = userService.createUser(newUser);
        
        ApiResponse<?> response = ApiResponse.builder()
                .success(true)
                .message("User created successfully")
                .statusCode(HttpStatus.CREATED.value())
                .data(mapToResponse(createdUser))
                .build();
        
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * ADMIN: Update user by ID (ADMIN only)
     */
    @PutMapping("/admin/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> updateUserByAdmin(
            @PathVariable Long userId,
            @Valid @RequestBody User updatedUser) {
        
        updatedUser.setId(userId);
        userService.updateUser(updatedUser);
        
        User user = userService.getUserById(userId);
        
        ApiResponse<?> response = ApiResponse.builder()
                .success(true)
                .message("User updated successfully")
                .statusCode(HttpStatus.OK.value())
                .data(mapToResponse(user))
                .build();
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * ADMIN: Delete user by ID (ADMIN only)
     */
    @DeleteMapping("/admin/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> deleteUserByAdmin(@PathVariable Long userId) {
        userService.deleteUser(userId);
        
        ApiResponse<?> response = ApiResponse.builder()
                .success(true)
                .message("User deleted successfully")
                .statusCode(HttpStatus.OK.value())
                .build();
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
