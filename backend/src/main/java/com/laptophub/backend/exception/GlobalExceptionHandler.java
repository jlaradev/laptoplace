package com.laptophub.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e,
            HttpServletRequest request) {
        
        // Extrae todos los mensajes de error de validación
        String errorMessage = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        
        // Si no hay errores de campo, intenta con errores globales
        if (errorMessage.isEmpty()) {
            errorMessage = e.getBindingResult()
                    .getGlobalErrors()
                    .stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.joining("; "));
        }
        
        return ResponseEntity.status(400)
            .body(ApiResponse.builder()
                .success(false)
                .message(errorMessage.isEmpty() ? "Validation failed" : errorMessage)
                .data(null)
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build());
    }
    
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
