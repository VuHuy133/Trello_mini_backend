package com.trello.exception;

public class UnauthorizedAccessException extends RuntimeException {
    private String resourceType;
    private Long resourceId;
    private Long userId;

    public UnauthorizedAccessException(String message) {
        super(message);
    }

    public UnauthorizedAccessException(String resourceType, Long resourceId, Long userId) {
        super(String.format("User %d does not have permission to access %s with ID %d", userId, resourceType, resourceId));
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.userId = userId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public Long getUserId() {
        return userId;
    }
}
