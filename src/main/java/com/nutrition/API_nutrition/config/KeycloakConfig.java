package com.nutrition.API_nutrition.config;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fournit un bean Keycloak configuré pour interagir avec l'API Admin de Keycloak.
 * Fonctions principales:
 * <ul>
 *     <li>Charge la configuration Keycloak depuis les propriétés de l'application</li>
 *     <li>Crée et configure un client administrateur Keycloak</li>
 *     <li>Expose ce client comme un bean Spring pour injection de dépendance</li>
 * </ul>
 * <p>
 * Ce bean est utilisé par KeycloakService pour effectuer des opérations administratives sur Keycloak (créer des utilisateurs, gérer les rôles, etc.).
 */
@Configuration
public class KeycloakConfig {

    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.realm.client-id}")
    private String clientId;

    @Value("${keycloak.realm.client-secret}")
    private String clientSecret;

    /**
     * Créer un Bean de configuration utilisé dans le Framework
     * Explication :
     * Dans la classe KeycloakService Spring injecte l'objet l'instant Keycloak dans keycloakAdmin
     * qui reçoit la configuration en attente d'être implementer.
     * <p>
     * Cela donne à cette classe, la responsabilité de la configuration de l'accès au client keycloak et donne l'accès
     * cette configuration dans tout l'API.
     */
    @Bean
    public Keycloak keycloakAdmin() {
        return KeycloakBuilder.builder()
                .serverUrl(authServerUrl)
                .realm(realm)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();
    }
}
