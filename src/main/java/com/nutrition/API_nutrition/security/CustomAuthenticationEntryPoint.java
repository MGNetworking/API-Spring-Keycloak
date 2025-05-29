package com.nutrition.API_nutrition.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutrition.API_nutrition.model.response.GenericApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        // Construction de l'objet d'erreur structuré
        GenericApiErrorResponse<Void> errorResponse = new GenericApiErrorResponse<>(
                HttpStatus.UNAUTHORIZED,
                HttpStatus.UNAUTHORIZED.value(),
                "Token invalide ou issuer incorrect.",
                request.getRequestURI(),
                null, // Pas de données
                "AUTH_INVALID_ISSUER" // Code d'erreur technique
        );

        log.error("Une erreur est survenu pendant le procèssus d'authentification {}",
                authException.getMessage(), authException);

        // Configuration de la réponse HTTP
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");

        // Sérialiser l'objet en JSON
        ObjectMapper mapper = new ObjectMapper();
        response.getWriter().write(mapper.writeValueAsString(errorResponse));
    }

}
