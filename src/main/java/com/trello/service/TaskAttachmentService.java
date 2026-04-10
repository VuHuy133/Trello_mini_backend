package com.trello.service;

import com.trello.entity.TaskAttachment;
import com.trello.repository.TaskAttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskAttachmentService {
    private final TaskAttachmentRepository taskAttachmentRepository;

    public List<TaskAttachment> fetchTaskAttachments() {
        return this.taskAttachmentRepository.findAll();
    }
}
