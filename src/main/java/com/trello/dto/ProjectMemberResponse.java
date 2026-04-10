package com.trello.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMemberResponse {
    
    private Long id;
    private Long projectId;
    private Long userId;
    private String username;
    private String email;
    private String role;
    private LocalDateTime joinedAt;
}
