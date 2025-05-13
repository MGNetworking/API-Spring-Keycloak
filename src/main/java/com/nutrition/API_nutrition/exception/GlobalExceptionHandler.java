package com.nutrition.API_nutrition.exception;

import com.nutrition.API_nutrition.model.dto.ApiResponseData;
import com.nutrition.API_nutrition.model.response.GenericApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.net.BindException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Gestionnaire d'exception personnalisé pour les exceptions metier
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<GenericApiErrorResponse<ApiResponseData>> handleApiException(
            ApiException apiException,
            HttpServletRequest request) {

        log.error("Unhandled ApiException: {}", apiException.getMessage(), apiException);

        GenericApiErrorResponse<ApiResponseData> response = new GenericApiErrorResponse<>(
                apiException.getHttpStatus(),
                apiException.getHttpStatus().value(),
                apiException.getMessage(),
                request.getRequestURI(),
                null,
                apiException.getErrorCode()
        );

        return ResponseEntity
                .status(apiException.getHttpStatus().value())
                .body(response);
    }

    /**
     * Gestion des exceptions génériques
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericApiErrorResponse<ApiResponseData>> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unhandled exception: {}", ex.getMessage(), ex);

        GenericApiErrorResponse<ApiResponseData> response = new GenericApiErrorResponse<>(
                HttpStatus.INTERNAL_SERVER_ERROR,
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error has occurred",
                request.getRequestURI(),
                null,
                "UNEXPECTED_ERROR"
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    /**
     * Gestionnaire pour les exceptions HTTP communes
     */
    @ExceptionHandler({
            MissingRequestHeaderException.class,
            HttpMessageNotReadableException.class,
            BindException.class,
    })
    public ResponseEntity<GenericApiErrorResponse<Map<String, Object>>> handleHttpRequestExceptions(Exception ex, HttpServletRequest request) {

        log.error("Unhandled http generic exception: {}", ex.getMessage(), ex);

        Map<String, Object> data = new LinkedHashMap<>();

        // Message par défaut pour toutes ces exceptions
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String errorCode = "BAD_REQUEST";
        String message = "Bad request";

        // Message Personnalisé en fonction du type d'exception
        if (ex instanceof MissingRequestHeaderException) {
            data.put("message", "En-tête manquant: " + ((MissingRequestHeaderException) ex).getHeaderName());
            errorCode = "MISSING_HEADER";
            message = "Missing required header";

        } else if (ex instanceof HttpMessageNotReadableException) {
            data.put("message", "Corps de requête invalide ou manquant");
            errorCode = "INVALID_REQUEST_BODY";
            message = "Invalid request body";

        } else if (ex instanceof BindException) {
            data.put("message", "Binding error");
            errorCode = "BINDING_ERROR";
            message = "Request binding error";

        } else {
            data.put("message", ex.getMessage());
        }

        GenericApiErrorResponse<Map<String, Object>> response = new GenericApiErrorResponse<>(
                status,
                status.value(),
                "An unexpected http error has occurred",
                request.getRequestURI(),
                data,
                "UNEXPECTED_HTTP_ERROR"
        );

        return ResponseEntity.status(status).body(response);
    }

    /**
     * Gestionnaire pour les exceptions
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericApiErrorResponse<Map<String, Object>>> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        BindingResult result = ex.getBindingResult();

        // Cas typique : erreurs sur des champs d’un DTO
        if (result.hasFieldErrors()) {
            List<String> validationErrors = result.getFieldErrors()
                    .stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .collect(Collectors.toList());

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("errors", validationErrors);

            return ResponseEntity.badRequest().body(new GenericApiErrorResponse<>(
                    HttpStatus.BAD_REQUEST,
                    HttpStatus.BAD_REQUEST.value(),
                    "DTO validation failed",
                    request.getRequestURI(),
                    data,
                    "VALIDATION_ERROR"
            ));
        }

        // Sinon, on est peut-être dans un autre cas d'erreur (ex : internal error de Spring)
        Map<String, Object> data = Map.of("message", "Unexpected validation failure");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new GenericApiErrorResponse<>(
                HttpStatus.INTERNAL_SERVER_ERROR,
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Unexpected validation context",
                request.getRequestURI(),
                data,
                "UNEXPECTED_VALIDATION_CONTEXT"
        ));
    }

}
