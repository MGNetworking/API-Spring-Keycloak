package com.nutrition.API_nutrition.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Fournit un bean Keycloak configuré pour interagir avec l'API Admin de Keycloak.
 * Fonctions principales :
 * <ul>
 *     <li>Charge la configuration Keycloak depuis les propriétés de l'application</li>
 *     <li>Crée et configure un client administrateur Keycloak</li>
 *     <li>Expose ce client comme un bean Spring pour injection de dépendance</li>
 * </ul>
 * <p>
 * Ce bean est utilisé par KeycloakService pour effectuer des opérations administratives sur Keycloak (créer des utilisateurs, gérer les rôles, etc.).
 */
@Slf4j
@Component
public class KeycloakProvider {

    @Getter
    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;

    @Getter
    @Value("${keycloak.realm}")
    private String realm;

    @Getter
    @Value("${keycloak.realm.client-id}")
    private String clientId;

    @Getter
    @Value("${keycloak.realm.client-secret}")
    private String clientSecret;

    private static Keycloak keycloakInstance = null;

    @PostConstruct
    public void init() {
        if (keycloakInstance == null) {
            keycloakInstance = createKeycloakInstance();
        }
    }

    public Keycloak getKeycloakInstance() {
        init();
        return keycloakInstance;
    }


    private Keycloak createKeycloakInstance() {
        return KeycloakBuilder.builder()
                .serverUrl(authServerUrl)
                .realm(realm)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();
    }


    public void logKeycloakConfig() {
        log.debug("Keycloak Configuration");
        log.debug("Server URL: {}", authServerUrl);
        log.debug("Master Realm: {}", realm);
        log.debug("Client ID: {}", clientId);
        log.debug("Client Secret is set: {}", clientSecret != null && !clientSecret.isEmpty());
    }
}

