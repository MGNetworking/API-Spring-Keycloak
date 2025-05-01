package com.nutrition.API_nutrition.exception;

import com.nutrition.API_nutrition.model.dto.ApiResponseData;
import com.nutrition.API_nutrition.model.response.GenericApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * Gestionnaire d'exception personnalisé pour les exceptions metier
     *
     * @param apiException
     * @param request
     * @return
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
                null, // data
                apiException.getErrorCode()
        );

        return ResponseEntity
                .status(apiException.getHttpStatus().value())
                .body(response);
    }

    /**
     * @param ex
     * @return
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
}
