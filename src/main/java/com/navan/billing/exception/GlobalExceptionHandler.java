package com.navan.billing.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.navan.billing.model.ApiError;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateTransactionException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ApiError duplicate(DuplicateTransactionException ex) {
        return ApiError.builder()
                .error("duplicate_external_id")
                .message(ex.getMessage())
                .existingId(ex.existingId())
                .build();
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ApiError conflict(ConflictException ex) {
        return ApiError.of(ex.code(), ex.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ApiError notFound(NotFoundException ex) {
        return ApiError.of("not_found", ex.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiError badRequest(BadRequestException ex) {
        return ApiError.builder()
                .error("validation_failed")
                .message(ex.getMessage())
                .details(ex.details().isEmpty() ? null : ex.details())
                .build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiError invalidBody(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + " " + e.getDefaultMessage())
                .sorted(Comparator.naturalOrder())
                .toList();
        return ApiError.builder()
                .error("validation_failed")
                .message("request body is invalid")
                .details(details)
                .build();
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiError unreadable(HttpMessageNotReadableException ex) {
        if (ex.getCause() instanceof JsonMappingException jme && !jme.getPath().isEmpty()) {
            return ApiError.of("validation_failed", "invalid value at " + pathOf(jme) + ": " + jme.getOriginalMessage());
        }
        return ApiError.of("malformed_request", "request body is not valid JSON");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiError typeMismatch(MethodArgumentTypeMismatchException ex) {
        return ApiError.of("validation_failed", "invalid value for '" + ex.getName() + "'");
    }

    // Constraint violations (SQLState 23xxx: unique, FK RESTRICT) mean a concurrent request got there first.
    // Anything else (overflow, value too long) is a server-side bug and must not look retryable.
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> integrity(DataIntegrityViolationException ex) {
        if (isConstraintViolation(ex)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiError.of("conflict", "request conflicts with current state; reload and retry"));
        }
        log.error("unexpected data error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of("internal_error", "the request could not be stored"));
    }

    private static boolean isConstraintViolation(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof SQLException sql && sql.getSQLState() != null) {
                return sql.getSQLState().startsWith("23");
            }
        }
        return false;
    }

    @ExceptionHandler(ConcurrencyFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ApiError concurrency(ConcurrencyFailureException ex) {
        return ApiError.of("concurrent_modification", "resource is being modified concurrently; retry");
    }

    static String pathOf(JsonMappingException ex) {
        return ex.getPath().stream()
                .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : "[" + ref.getIndex() + "]")
                .collect(Collectors.joining("."))
                .replace(".[", "[");
    }
}
