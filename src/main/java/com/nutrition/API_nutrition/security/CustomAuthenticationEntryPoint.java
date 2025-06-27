package com.nutrition.API_nutrition.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutrition.API_nutrition.exception.ErrorCode;
import com.nutrition.API_nutrition.model.response.GenericApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.io.IOException;

import static com.nutrition.API_nutrition.exception.ErrorCode.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper mapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        GenericApiErrorResponse<Void> errorResponse;
        ErrorCode errorCode = AUTH_JWT_ERROR;
        String message = "Erreur d'authentification";


        if (authException.getCause() instanceof JwtException jwtException) {
            if (jwtException instanceof BadJwtException) {
                errorCode = AUTH_MALFORMED_TOKEN;
            } else if (jwtException.getMessage().toLowerCase().contains("invalid_audience")) {
                errorCode = AUTH_INVALID_AUDIENCE;
            } else if (jwtException.getMessage().toLowerCase().contains("signature") || jwtException.getMessage().toLowerCase().contains("issuer")) {
                errorCode = AUTH_INVALID_SIGNATURE_OR_ISSUER;
            } else if (jwtException.getMessage().toLowerCase().contains("expired")) {
                errorCode = AUTH_EXPIRED_TOKEN;
            } else if (jwtException.getCause() instanceof RestClientException) {
                errorCode = AUTH_JWK_RETRIEVAL_ERROR;
            }
        } else if (authException.getMessage().toLowerCase().contains("bearer")) {
            errorCode = AUTH_MISSING_TOKEN;
        } else {
            errorCode = AUTH_GENERIC_ERROR;
        }

        // Construction de la réponse d'erreur
        errorResponse = new GenericApiErrorResponse<>(
                HttpStatus.UNAUTHORIZED,
                HttpStatus.UNAUTHORIZED.value(),
                message,
                request.getRequestURI(),
                null,
                errorCode.name()
        );

        log.error("Erreur d'authentification : {} ({})", authException.getMessage(), errorCode, authException);

        // Configuration de la réponse JSON
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write(mapper.writeValueAsString(errorResponse));
        response.getWriter().flush();
    }

}
