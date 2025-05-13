package com.nutrition.API_nutrition.security;

import com.nutrition.API_nutrition.service.KeycloakService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.KeycloakPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccessKeycloak {

    private final KeycloakService keycloakService;

    /**
     * Lecture depuis SecurityContext, comparaison ID token vs userId
     *
     * @param userId l'identifiant utilisateur
     * @return {@code true} si l'identifiant est valide, {@code false} sinon.
     */
    public boolean hasAccessToUser(String userId) {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        log.info("[AccessKeycloak] check user id access: {}", userId);
        log.info("Authentication principal: {}", authentication.getPrincipal());

        String tokenUserId = getUserIdFromToken(authentication);

        if (userId.equals(tokenUserId)) {
            log.info("Authorised access: ID correspond.");
            return true;
        }

        log.warn("Access denied: ID does not match.");
        return false;

    }

    /**
     * Appel vers Keycloak, demande de validation.
     *
     * @return {@code true} si le token est valide, {@code false} sinon
     */
    public boolean isTokenValid() {

        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
            log.warn("Authentication is null or not instance Jwt: {}", authentication);
            return false;
        }

        log.info("Authentication principal: {}", authentication.getPrincipal());

        Jwt jwt = (Jwt) authentication.getPrincipal();
        String token = jwt.getTokenValue();


        log.info("serialization of the jwt token {}", token);
        return this.keycloakService.validateToken(token);

    }

    /**
     * Permet de verifier l'identité utilisateur ainsi que la validité du token
     *
     * @param userId l'identifiant utilisateur
     * @return {@code true} si l'identité et token est valide, {@code false} sinon
     */
    public boolean isAuthenticatedAndAuthorized(String userId) {
        return hasAccessToUser(userId) && isTokenValid();
    }


    private String getUserIdFromToken(Authentication authentication) {
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaimAsString("sub");
        }

        if (authentication.getPrincipal() instanceof KeycloakPrincipal<?> kcPrincipal) {
            return kcPrincipal.getName(); // ou kcPrincipal.getKeycloakSecurityContext().getToken().getSubject();
        }

        return authentication.getName(); // fallback
    }

}
