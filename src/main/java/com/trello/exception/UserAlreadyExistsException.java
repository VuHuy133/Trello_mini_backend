package com.trello.exception;

public class UserAlreadyExistsException extends RuntimeException {
    private String fieldName;
    private String fieldValue;

    public UserAlreadyExistsException(String fieldName, String fieldValue) {
        super(String.format("User with %s '%s' already exists", fieldName, fieldValue));
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getFieldValue() {
        return fieldValue;
    }
}
