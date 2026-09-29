package com.pingprint.api;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    public record ApiError(Instant timestamp, int status, String code, String message, String path) { }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiError validation(MethodArgumentNotValidException error, HttpServletRequest request) {
        String message = error.getBindingResult().getFieldErrors().stream().findFirst()
            .map(e -> e.getField() + ": " + e.getDefaultMessage()).orElse("Invalid request");
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message, request);
    }
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiError badRequest(IllegalArgumentException error, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, code(error.getMessage()), safe(error.getMessage()), request);
    }
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ApiError conflict(IllegalStateException error, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, code(error.getMessage()), safe(error.getMessage()), request);
    }
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    ApiError forbidden(AccessDeniedException error, HttpServletRequest request) {
        return response(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "You do not have permission to perform this action.", request);
    }
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    ApiError unexpected(Exception error, HttpServletRequest request) {
        log.error("Unhandled API failure on {}", request.getRequestURI(), error);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "The server could not complete the request.", request);
    }
    private ApiError response(HttpStatus status, String code, String message, HttpServletRequest request) {
        return new ApiError(Instant.now(), status.value(), code, message, request.getRequestURI());
    }
    private String safe(String message) { return message == null || message.isBlank() ? "The request could not be completed." : message; }
    private String code(String message) {
        String value = safe(message).toLowerCase();
        if (value.contains("printer") && value.contains("unavailable")) return "PRINTER_NOT_AVAILABLE";
        if (value.contains("payment")) return "PAYMENT_ERROR";
        if (value.contains("media")) return "MEDIA_NOT_AVAILABLE";
        if (value.contains("reauthor")) return "PRINTER_REAUTHORIZATION_REQUIRED";
        if (value.contains("document") || value.contains("pdf") || value.contains("file")) return "DOCUMENT_ERROR";
        return "INVALID_REQUEST";
    }
}
