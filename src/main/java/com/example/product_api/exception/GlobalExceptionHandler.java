package com.example.product_api.exception;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", ex.getMessage());
        body.put("statusCode", HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(ConflictException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", ex.getMessage());
        body.put("statusCode", HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<Map<String, Object>> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toErrorItem)
                .collect(Collectors.toList());

        Map<String, Object> body = jsonBody(HttpStatus.BAD_REQUEST, "Erro de validação", request.getRequestURI(), errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, Object>> handleBindException(BindException ex, HttpServletRequest request) {
    List<Map<String, Object>> errors = ex.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(this::toErrorItem)
        .collect(Collectors.toList());

    Map<String, Object> body = jsonBody(HttpStatus.BAD_REQUEST, "Erro de validação", request.getRequestURI(), errors);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
    List<Map<String, Object>> errors = ex.getConstraintViolations()
        .stream()
        .map(this::toErrorItem)
        .collect(Collectors.toList());

    Map<String, Object> body = jsonBody(HttpStatus.BAD_REQUEST, "Erro de validação", request.getRequestURI(), errors);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException ex, HttpServletRequest request) {
        Map<String, Object> body = jsonBody(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    private Map<String, Object> toErrorItem(FieldError fe) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("field", fe.getField());
        item.put("message", fe.getDefaultMessage());
        item.put("rejectedValue", fe.getRejectedValue());
        return item;
    }

    private Map<String, Object> toErrorItem(ConstraintViolation<?> cv) {
        String field = cv.getPropertyPath() != null ? cv.getPropertyPath().toString() : null;
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("field", field);
        item.put("message", cv.getMessage());
        item.put("rejectedValue", cv.getInvalidValue());
        return item;
    }

    private Map<String, Object> jsonBody(HttpStatus status, String message, String path, List<Map<String, Object>> errors) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", path);
        if (errors != null) {
            body.put("errors", errors);
        }
        return body;
    }
}