package com.personaflow.commerce.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    COMMON_VALIDATION_FAILED("COMMON_VALIDATION_FAILED", HttpStatus.BAD_REQUEST, "Request validation failed"),
    COMMON_BAD_REQUEST("COMMON_BAD_REQUEST", HttpStatus.BAD_REQUEST, "Bad request"),
    COMMON_INTERNAL_ERROR("COMMON_INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),

    ACCOUNT_USERNAME_EXISTS("ACCOUNT_USERNAME_EXISTS", HttpStatus.CONFLICT, "Username already exists"),
    ACCOUNT_INVALID_CREDENTIALS("ACCOUNT_INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "Invalid username or password"),
    ACCOUNT_UNAUTHORIZED("ACCOUNT_UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "Authentication is required"),
    ACCOUNT_FORBIDDEN("ACCOUNT_FORBIDDEN", HttpStatus.FORBIDDEN, "Access is denied"),
    ACCOUNT_CURRENT_PASSWORD_INVALID("ACCOUNT_CURRENT_PASSWORD_INVALID", HttpStatus.BAD_REQUEST, "Current password is invalid"),
    ACCOUNT_PASSWORD_INVALID("ACCOUNT_PASSWORD_INVALID", HttpStatus.BAD_REQUEST, "Password format is invalid"),
    ADDRESS_NOT_FOUND("ADDRESS_NOT_FOUND", HttpStatus.NOT_FOUND, "Address not found"),
    ADDRESS_NOT_OWNED("ADDRESS_NOT_OWNED", HttpStatus.FORBIDDEN, "Address does not belong to current user");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(String code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public String code() {
        return code;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
