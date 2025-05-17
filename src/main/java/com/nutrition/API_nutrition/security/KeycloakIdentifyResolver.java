package com.nutrition.API_nutrition.security;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.KeycloakPrincipal;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class KeycloakIdentifyResolver implements UserIdentifyResolver {

    @Override
    public boolean support(Object principal) {
        return principal instanceof KeycloakPrincipal<?> kcPrincipal;
    }

    @Override
    public Optional<String> resolver(Object principal) {

        if (principal == null) {
            return Optional.empty();
        }

        KeycloakPrincipal<?> kcPrincipal = (KeycloakPrincipal<?>) principal;
        String subject = kcPrincipal.getKeycloakSecurityContext().getToken().getSubject();
        log.info("Keycloak principal subject: {}", subject);
        return Optional.of(subject);
    }
}
