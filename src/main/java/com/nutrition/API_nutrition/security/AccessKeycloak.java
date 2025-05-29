package com.nutrition.API_nutrition.security;

import com.nutrition.API_nutrition.exception.ApiException;
import com.nutrition.API_nutrition.exception.ErrorCode;
import com.nutrition.API_nutrition.service.KeycloakService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccessKeycloak {

    private final List<UserIdentifyResolver> lsResolvers;

    /**
     * Lecture depuis SecurityContext, comparaison ID token vs userId
     *
     * @param userId l'identifiant utilisateur
     * @return {@code true} si l'identifiant est valide, {@code false} sinon.
     */
    public boolean hasAccessToUser(String userId) {

        String tokenUserId = getUserIdFromToken();
        log.info("[AccessKeycloak] check user id access: {} and token User Id: {}", userId, tokenUserId);

        if (userId.equals(tokenUserId)) {
            log.info("Authorised access: ID correspond.");
            return true;
        }

        log.warn("Access denied: ID does not match.");
        return false;

    }


    public String getUserIdFromToken() {

        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException(
                    "The authentication token is missing or invalid",
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.AUTHENTICATED_BAD_REQUEST.toString());

        }

        Object principal = authentication.getPrincipal();
        log.info("Authentication principal: {}", principal);

        for (UserIdentifyResolver userResolver : lsResolvers) {
            log.info("Resolved type: {}", userResolver.getClass());

            if (userResolver.support(principal)) {
                Optional<String> resolver = userResolver.resolver(principal);

                if (resolver.isPresent()) {
                    String userId = resolver.get();
                    log.info("User ID is: {}", userId);
                    return userId;
                }
            }
        }

        throw new ApiException(
                "Unsupported authentication principal type: " + principal.getClass().getName(),
                HttpStatus.UNAUTHORIZED,
                ErrorCode.AUTHENTICATED_BAD_REQUEST.toString());
    }

    public String extractToken(String token) throws ApiException {
        return Optional.ofNullable(token)
                .filter(header -> header.startsWith("Bearer "))
                .map(header -> header.replace("Bearer ", ""))
                .orElseThrow(() -> new ApiException("Invalid Authorization header",
                        HttpStatus.BAD_REQUEST,
                        ErrorCode.AUTHENTICATED_BAD_REQUEST.toString()));


    }

}
