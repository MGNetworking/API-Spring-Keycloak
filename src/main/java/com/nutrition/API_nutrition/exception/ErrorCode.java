package com.nutrition.API_nutrition.exception;

public enum ErrorCode {
    USER_CREATION_FAILED,
    USER_ROLE_ASSIGNMENT_FAILED,
    ROLE_NOT_FOUND,
    USER_ALREADY_EXISTS,
    KEYCLOAK_CONNECTION_FAILED,
    NETWORK_ERROR,
    DB_ERROR
}
