package ru.practicum.ewm.main.error;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class ErrorHandler {
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException exception) {
        log.warn("Объект не найден: {}", exception.getMessage());
        return buildError(HttpStatus.NOT_FOUND, exception.getMessage(),
                "The required object was not found.", List.of());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException exception) {
        log.warn("Конфликт при выполнении запроса: {}", exception.getMessage());
        return buildError(HttpStatus.CONFLICT, exception.getMessage(),
                "For the requested operation the conditions are not met.", List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException exception) {
        log.warn("Нарушение целостности данных: {}", exception.getMessage());
        return buildError(HttpStatus.CONFLICT, exception.getMessage(),
                "Integrity constraint has been violated.", List.of());
    }

    @ExceptionHandler({BadRequestException.class, ConstraintViolationException.class,
            MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class})
    public ResponseEntity<ApiError> handleBadRequest(Exception exception) {
        log.warn("Некорректный запрос: {}", exception.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, exception.getMessage(),
                "Incorrectly made request.", List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        List<String> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> "Field: " + error.getField() + ". Error: " + error.getDefaultMessage())
                .toList();
        String message = errors.isEmpty() ? "Validation failed" : errors.getFirst();
        log.warn("Ошибка валидации: {}", message);
        return buildError(HttpStatus.BAD_REQUEST, message, "Incorrectly made request.", errors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleOther(Exception exception) {
        log.error("Внутренняя ошибка сервера", exception);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(),
                "Internal server error.", List.of());
    }

    private ResponseEntity<ApiError> buildError(HttpStatus status, String message,
                                                String reason, List<String> errors) {
        ApiError error = new ApiError(errors, message, reason, status, LocalDateTime.now());
        return ResponseEntity.status(status).body(error);
    }
}
