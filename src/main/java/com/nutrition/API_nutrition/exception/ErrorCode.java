package com.nutrition.API_nutrition.exception;

/**
 * Permet de centraliser les codes erreurs retourné au client
 */
public enum ErrorCode {
    INVALID_USER_ID,
    INVALID_USER_DATA,
    USER_ROLE_ASSIGNMENT_FAILED,
    KEYCLOAK_BAD_REQUEST,
    KEYCLOAK_UNAUTHORIZED,
    KEYCLOAK_FORBIDDEN,
    AUTHENTICATED_BAD_REQUEST,
    DB_USER_NOT_FOUND,
    DB_SYSTEM_ERROR,
    DB_CONSTRAINT_VIOLATION,
    DB_ERROR,
    TECHNICAL_ERROR,
    BAD_REQUEST_PARAMETER
}
