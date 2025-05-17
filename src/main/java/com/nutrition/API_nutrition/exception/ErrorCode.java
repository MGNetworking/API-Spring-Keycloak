package com.nutrition.API_nutrition.exception;

/**
 * Permet de centraliser les codes erreurs retourné au client
 */
public enum ErrorCode {
    USER_CREATION_FAILED,
    USER_RESEARCH_FAILED,
    USER_UPDATE_FAILED,
    USER_ROLE_ASSIGNMENT_FAILED,
    KEYCLOAK_BAD_REQUEST,
    KEYCLOAK_UNAUTHORIZED,
    KEYCLOAK_FORBIDDEN,
    KEYCLOAK_USER_NOT_FOUND,
    KEYCLOAK_UNEXPECTED_ERROR,
    AUTHENTICATED_BAD_REQUEST,
    NETWORK_ERROR, // erreur réseau
    DB_ERROR, // Erreur en base de donnée
    TECHNICAL_ERROR, // Exception générale
    BAD_REQUEST_PARAMETER //
}
