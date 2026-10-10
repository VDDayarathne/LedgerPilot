package com.ledgerpilot.ledgerpilot_backend.common.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestControllerAdvice
public class ApiExceptionHandler {
    public record ErrorBody(Instant timestamp, int status, String code, String message, String path,
                            Map<String, String> fields) {
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorBody> api(ApiException ex, HttpServletRequest req) {
        return ResponseEntity.status(ex.status()).body(new ErrorBody(Instant.now(), ex.status().value(), ex.code(), ex.getMessage(), req.getRequestURI(), Map.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorBody> validation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> fields.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ErrorBody(Instant.now(), 400, "VALIDATION_FAILED", "Please correct the highlighted fields.", req.getRequestURI(), fields));
    }

    @ExceptionHandler({org.springframework.dao.DataIntegrityViolationException.class, org.springframework.orm.ObjectOptimisticLockingFailureException.class})
    ResponseEntity<ErrorBody> conflict(Exception ex, HttpServletRequest req) {
        return ResponseEntity.status(409).body(new ErrorBody(Instant.now(), 409, "CONFLICT", "The request conflicts with a recent change.", req.getRequestURI(), Map.of()));
    }
}
