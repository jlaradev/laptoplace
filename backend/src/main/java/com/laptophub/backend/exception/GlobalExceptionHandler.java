package com.laptophub.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleNotFound(
            ResourceNotFoundException e,
            HttpServletRequest request) {
        return ResponseEntity.status(404)
            .body(ApiResponse.builder()
                .success(false)
                .message(e.getMessage())
                .data(null)
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build());
    }
    
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<?>> handleConflict(
            ConflictException e,
            HttpServletRequest request) {
        return ResponseEntity.status(409)
            .body(ApiResponse.builder()
                .success(false)
                .message(e.getMessage())
                .data(null)
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build());
    }
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<?>> handleValidation(
            ValidationException e,
            HttpServletRequest request) {
        return ResponseEntity.status(400)
            .body(ApiResponse.builder()
                .success(false)
                .message(e.getMessage())
                .data(null)
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build());
    }
}
