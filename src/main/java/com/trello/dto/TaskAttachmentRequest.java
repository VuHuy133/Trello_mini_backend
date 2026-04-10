package com.trello.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskAttachmentRequest {
    
    private String fileName;
    private String fileUrl;
    private String fileType;
}
