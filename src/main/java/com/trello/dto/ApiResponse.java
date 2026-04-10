package com.trello.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private int statusCode;
    private T data;
    private Map<String, String> errors;
    private LocalDateTime timestamp;

    public ApiResponse(boolean success, String message, int statusCode) {
        this.success = success;
        this.message = message;
        this.statusCode = statusCode;
        this.timestamp = LocalDateTime.now();
    }

    public ApiResponse(boolean success, String message, int statusCode, T data) {
        this.success = success;
        this.message = message;
        this.statusCode = statusCode;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }
}
