package com.trello.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectResponse {
    
    private Long id;
    private String name;
    private String description;
    private Long ownerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int memberCount;
    private int taskCount;
    private String type;
    
    // Danh sách các thành viên đã tham gia dự án
    private List<ProjectMemberResponse> members;
    
    // Flag để kiểm tra user hiện tại đã tham gia dự án hay chưa
    private boolean joinedByCurrentUser;
}
