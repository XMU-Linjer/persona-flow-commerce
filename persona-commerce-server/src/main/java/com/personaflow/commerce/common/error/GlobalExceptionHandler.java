package com.personaflow.commerce.common.error;

import com.personaflow.commerce.common.api.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.errorCode();
        return ResponseEntity
                .status(errorCode.httpStatus())
                .body(ApiResponse.failure(errorCode, exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<String>> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(this::formatFieldError)
                .orElse(ErrorCode.COMMON_VALIDATION_FAILED.defaultMessage());
        return validationFailure(message);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<String>> handleHandlerMethodValidation(HandlerMethodValidationException exception) {
        String message = exception.getAllErrors()
                .stream()
                .findFirst()
                .map(MessageSourceResolvable::getDefaultMessage)
                .orElse(ErrorCode.COMMON_VALIDATION_FAILED.defaultMessage());
        return validationFailure(message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<String>> handleConstraintViolation(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));
        return validationFailure(message.isBlank() ? ErrorCode.COMMON_VALIDATION_FAILED.defaultMessage() : message);
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception exception) {
        return ResponseEntity
                .status(ErrorCode.COMMON_BAD_REQUEST.httpStatus())
                .body(ApiResponse.failure(ErrorCode.COMMON_BAD_REQUEST, exception.getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException exception) {
        return ResponseEntity
                .status(ErrorCode.ACCOUNT_UNAUTHORIZED.httpStatus())
                .body(ApiResponse.failure(ErrorCode.ACCOUNT_UNAUTHORIZED));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException exception) {
        return ResponseEntity
                .status(ErrorCode.ACCOUNT_FORBIDDEN.httpStatus())
                .body(ApiResponse.failure(ErrorCode.ACCOUNT_FORBIDDEN));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
        return ResponseEntity
                .status(ErrorCode.COMMON_INTERNAL_ERROR.httpStatus())
                .body(ApiResponse.failure(ErrorCode.COMMON_INTERNAL_ERROR));
    }

    private ResponseEntity<ApiResponse<String>> validationFailure(String message) {
        return ResponseEntity
                .status(ErrorCode.COMMON_VALIDATION_FAILED.httpStatus())
                .body(ApiResponse.failure(ErrorCode.COMMON_VALIDATION_FAILED, message, message));
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }
}
