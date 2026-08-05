package com.financecontrol.finance.api;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import com.financecontrol.finance.service.ResourceNotFoundException;
import com.financecontrol.finance.service.DomainValidationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

    @ExceptionHandler(DomainValidationException.class)
    public ResponseEntity<ProblemDetail> handleDomainValidation(
            DomainValidationException exception,
            HttpServletRequest request) {
        var problemDetail = createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                exception.getMessage(),
                request);

        return ResponseEntity.badRequest().body(problemDetail);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request) {
        var problemDetail = createProblemDetail(
                HttpStatus.NOT_FOUND,
                "Resource not found",
                exception.getMessage(),
                request);

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        var problemDetail = createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                "One or more fields are invalid.",
                request);
        problemDetail.setProperty("errors", errors);

        return ResponseEntity.badRequest().body(problemDetail);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request) {
        var problemDetail = createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                "One or more parameters are invalid.",
                request);

        return ResponseEntity.badRequest().body(problemDetail);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ProblemDetail> handleMalformedRequest(
            Exception exception,
            HttpServletRequest request) {
        var problemDetail = createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Invalid request",
                "The request contains malformed or unsupported values.",
                request);

        return ResponseEntity.badRequest().body(problemDetail);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleConflict(
            DataIntegrityViolationException exception,
            HttpServletRequest request) {
        var problemDetail = createProblemDetail(
                HttpStatus.CONFLICT,
                "Data conflict",
                "The operation conflicts with the current data state.",
                request);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(
            NoResourceFoundException exception,
            HttpServletRequest request) {
        var problemDetail = createProblemDetail(
                HttpStatus.NOT_FOUND,
                "Resource not found",
                "The requested resource does not exist.",
                request);

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    private static ProblemDetail createProblemDetail(
            HttpStatus status,
            String title,
            String detail,
            HttpServletRequest request) {
        var problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        return problemDetail;
    }
}
