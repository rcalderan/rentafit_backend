package br.com.rentafit.common.exception;

import br.com.rentafit.common.util.EnvironmentUtil;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final boolean isProd = EnvironmentUtil.isProduction();

    // ==================== 4xx Errors ====================

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex,
                                                               HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error(isProd ? null : HttpStatus.NOT_FOUND.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> ErrorResponse.FieldError.builder()
                        .field(error.getField())
                        .message(error.getDefaultMessage())
                        .rejectedValue(error.getRejectedValue())
                        .build())
                .collect(Collectors.toList());

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(isProd ? "" : HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(isProd ? "" : "Validation failed for one or more fields")
                .path(request.getRequestURI())
                .errors(fieldErrors)
                .build();

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                      HttpServletRequest request) {
        List<ErrorResponse.FieldError> fieldErrors = new ArrayList<>();
        String message = "Invalid JSON format or type mismatch";

        if (ex.getCause() instanceof InvalidFormatException ife) {

            String fieldName = ife.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .filter(name -> name != null && !name.isEmpty())
                    .findFirst()
                    .orElse("unknown");

            String expectedType = ife.getTargetType().getSimpleName();
            Object receivedValue = ife.getValue();

            message = String.format(
                    "Field '%s' expects type '%s' but received '%s' (value: %s)",
                    fieldName,
                    expectedType,
                    receivedValue != null ? receivedValue.getClass().getSimpleName() : "null",
                    receivedValue
            );

            fieldErrors.add(ErrorResponse.FieldError.builder()
                    .field(fieldName)
                    .message(String.format("Invalid type. Expected %s", expectedType))
                    .rejectedValue(receivedValue)
                    .build());
        } else {
            String cause = ex.getMostSpecificCause().getMessage();
            message = isProd ? "Invalid JSON format" : cause;
        }

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(isProd ? "" : HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(isProd ? "Invalid request body" : message)
                .path(request.getRequestURI())
                .errors(fieldErrors.isEmpty() ? null : fieldErrors)
                .build();

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex,
                                                              HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(isProd ? "" : HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(isProd ? "Invalid argument" : ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Handles type-conversion failures for path and query parameters.
     * Example: a malformed UUID in /contracts/{id} returns 400 instead of 500.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                                          HttpServletRequest request) {
        String paramName  = ex.getName();
        Object rejected   = ex.getValue();
        String targetType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";

        String message = String.format(
                "Parâmetro '%s' com valor '%s' não pode ser convertido para o tipo '%s'",
                paramName, rejected, targetType);

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(isProd ? "" : HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(isProd ? "Invalid parameter type" : message)
                .path(request.getRequestURI())
                .errors(List.of(ErrorResponse.FieldError.builder()
                        .field(paramName)
                        .message("Tipo inválido. Esperado: " + targetType)
                        .rejectedValue(rejected)
                        .build()))
                .build();

        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Handles missing required query or path parameters.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
                                                                               HttpServletRequest request) {
        String message = String.format("Parâmetro obrigatório '%s' (tipo: %s) está ausente",
                ex.getParameterName(), ex.getParameterType());

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(isProd ? "" : HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(isProd ? "Missing required parameter" : message)
                .path(request.getRequestURI())
                .errors(List.of(ErrorResponse.FieldError.builder()
                        .field(ex.getParameterName())
                        .message("Parâmetro obrigatório ausente")
                        .rejectedValue(null)
                        .build()))
                .build();

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex,
                                                           HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(isProd ? "" : HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .message(isProd ? "Unauthorized" : ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex,
                                                               HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.METHOD_NOT_ALLOWED.value())
                .error(isProd ? "" : HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase())
                .message(isProd ? "Method not allowed" : ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoHandlerFoundException ex,
                                                       HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error(isProd ? "" : HttpStatus.NOT_FOUND.getReasonPhrase())
                .message(isProd ? "Not found" : "Endpoint not found")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // ==================== 422 Unprocessable Entity ====================

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleBusinessValidation(ValidationException ex,
                                                                  HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .error(isProd ? "" : HttpStatus.UNPROCESSABLE_ENTITY.getReasonPhrase())
                .message(isProd ? "" : ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    // ==================== 5xx Errors ====================

    @ExceptionHandler(ExternalServiceTimeoutException.class)
    public ResponseEntity<ErrorResponse> handleExternalServiceTimeout(ExternalServiceTimeoutException ex,
                                                                      HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.GATEWAY_TIMEOUT.value())
                .error(isProd ? null : HttpStatus.GATEWAY_TIMEOUT.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(body);
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ErrorResponse> handleExternalServiceError(ExternalServiceException ex,
                                                                   HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_GATEWAY.value())
                .error(isProd ? "" : HttpStatus.BAD_GATEWAY.getReasonPhrase())
                .message(isProd ? "External service error" : ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccessException(DataAccessException ex,
                                                                   HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(isProd ? "" : HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message(isProd ? "Database error occurred" : ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleInternal(Exception ex,
                                                        HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message(isProd ? "An unexpected error occurred" : ex.getMessage())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
