package alik.utilitymeter.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ExceptionResponse> handleNotFound(NotFoundException ex, HttpServletRequest request) {
    return handleException(ex, request, HttpStatus.NOT_FOUND, ex.getMessage(), false);
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<ExceptionResponse> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
    return handleException(ex, request, HttpStatus.UNAUTHORIZED, ex.getMessage(), false);
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ExceptionResponse> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
    return handleException(ex, request, HttpStatus.BAD_REQUEST, ex.getMessage(), false);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ExceptionResponse> handleConflict(DataIntegrityViolationException ex, HttpServletRequest request) {
    return handleException(ex, request, HttpStatus.CONFLICT, ex.getMessage(), false);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ExceptionResponse> handleValidation(MethodArgumentNotValidException e,
                                                            HttpServletRequest request) {
    String errors = e.getBindingResult().getFieldErrors().stream()
        .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
        .collect(Collectors.joining(", "));
    return handleException(e, request, HttpStatus.BAD_REQUEST, "Validation failed: " + errors,
        false);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ExceptionResponse> handleGlobalException(Exception ex, HttpServletRequest request) {
    return handleException(ex, request, HttpStatus.INTERNAL_SERVER_ERROR,
        "An unexpected error occurred on the server.", true);
  }

  private ResponseEntity<ExceptionResponse> handleException(
      Exception e,
      HttpServletRequest request,
      HttpStatus status,
      String message,
      boolean unexpected
  ) {
    if (unexpected) {
      logUnexpectedException(e, request);
    } else {
      logExpectedException(e, request);
    }

    return buildErrorResponse(
        null != message ? message : e.getMessage(),
        status,
        request.getRequestURI(),
        status.name()
    );
  }

  private ResponseEntity<ExceptionResponse> buildErrorResponse(String message,
                                                               HttpStatus status,
                                                               String path,
                                                               String code) {
    return ResponseEntity.status(status)
        .body(new ExceptionResponse(Instant.now(), status.value(), message, path, code));
  }

  private void logExpectedException(Exception e, HttpServletRequest request) {
    log.warn("{} {} threw {}: {}", request.getMethod(),
        request.getRequestURI(),
        e.getClass().getSimpleName(), e.getMessage());
  }

  private void logUnexpectedException(Exception e, HttpServletRequest request) {
    log.error("{} {} threw unexpected exception: {}", request.getMethod(),
        request.getRequestURI(),
        e.getMessage(), e);
  }
  
  
}