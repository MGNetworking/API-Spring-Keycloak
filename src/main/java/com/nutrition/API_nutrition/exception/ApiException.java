package com.nutrition.API_nutrition.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Classe de gestion des exceptions métier
 */
public class ApiException extends RuntimeException {

    @Getter
    private final String message;
    @Getter
    private final HttpStatus httpStatus;
    @Getter
    private final String errorCode;

    public ApiException(String msg, HttpStatus httpStatus, String errorCode) {
        super(String.format("Message: '%s' status: %s", msg, httpStatus));
        this.message = msg;
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;


    }

}
