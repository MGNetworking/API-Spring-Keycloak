package com.nutrition.API_nutrition.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutrition.API_nutrition.model.dto.TokenResponseDto;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakService {

    private final Keycloak keycloakAdmin;

    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.realm.client-id}")
    private String clientId;

    @Value("${keycloak.realm.client-secret}")
    private String clientSecret;


    /**
     * Crée un nouvel utilisateur dans Keycloak
     */
    public String createUser(String username, String email, String password, String firstName, String lastName) throws JsonProcessingException {

        // Création d'un objet de représentation pour l'authentification
        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);

        // Définir les credentials
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);

        // add list credentials
        user.setCredentials(List.of(credential));

        ObjectMapper mapper = new ObjectMapper();
        log.info("Utilisateur envoyé à Keycloak: {}", mapper.writeValueAsString(user));

        // Créer l'utilisateur
        Response response = keycloakAdmin.realm(realm).users().create(user);

        log.info("Response : {}", response);

        // check les réponses
        if (response.getStatus() < 200 || response.getStatus() >= 300) {
            log.error("status : {}", response.getStatus());
            throw new RuntimeException("Failed to create user in Keycloak: " + response.getStatusInfo());
        }

        // Extraire l'ID du nouvel utilisateur
        String locationPath = response.getLocation().getPath();
        log.info("locationPath : {}", locationPath);
        String userId = locationPath.substring(locationPath.lastIndexOf('/') + 1);
        log.info("user Id extract : {}", userId);

        return userId;
    }

    /**
     * Ajoute des rôles à un utilisateur
     */
    public void addUserRoles(String userId, List<String> roleNames) throws JsonProcessingException {

        log.info("Récupérer les représentations des rôles");
        List<RoleRepresentation> roles = roleNames.stream()
                .map(roleName -> {
                    log.info("Le role {}", roleName);
                    return this.keycloakAdmin.realm(realm).roles().get(roleName).toRepresentation();
                })
                .collect(Collectors.toList());

        ObjectMapper mapper = new ObjectMapper();
        log.info("Liste des roles keycloak: {}", mapper.writeValueAsString(roles));

        // Assigner les rôles à l'utilisateur
        this.keycloakAdmin.realm(realm).users().get(userId).roles().realmLevel().add(roles);
    }

    /**
     * Authentifie un utilisateur et retourne les tokens
     */
    public TokenResponseDto login(String username, String password) {
        try {
            String tokenUrl = authServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";

            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tokenUrl))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            "client_id=" + clientId +
                                    "&client_secret=" + clientSecret +
                                    "&grant_type=password" +
                                    "&username=" + URLEncoder.encode(username, StandardCharsets.UTF_8) +
                                    "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8)
                    ))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Authentication failed: " + response.body());
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(response.body());

            return new TokenResponseDto(
                    node.get("access_token").asText(),
                    node.get("refresh_token").asText(),
                    node.get("expires_in").asLong(),
                    node.get("refresh_expires_in").asLong()
            );
        } catch (Exception e) {
            throw new RuntimeException("Authentication process failed", e);
        }
    }

    /**
     * Rafraîchit un token expiré
     */
    public TokenResponseDto refreshToken(String refreshToken) {
        try {
            String tokenUrl = authServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";

            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tokenUrl))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            "client_id=" + clientId +
                                    "&client_secret=" + clientSecret +
                                    "&grant_type=refresh_token" +
                                    "&refresh_token=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8)
                    ))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Token refresh failed: " + response.body());
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(response.body());

            return new TokenResponseDto(
                    node.get("access_token").asText(),
                    node.get("refresh_token").asText(),
                    node.get("expires_in").asLong(),
                    node.get("refresh_expires_in").asLong()
            );
        } catch (Exception e) {
            throw new RuntimeException("Token refresh process failed", e);
        }
    }

    /**
     * Récupère les informations d'un utilisateur
     */
    public UserRepresentation getUserInfo(String userId) {
        return keycloakAdmin.realm(realm).users().get(userId).toRepresentation();
    }

    /**
     * Met à jour les informations d'un utilisateur
     */
    public void updateUser(String userId, UserRepresentation userRepresentation) {
        keycloakAdmin.realm(realm).users().get(userId).update(userRepresentation);
    }

    /**
     * Déconnecte un utilisateur
     */
    public void logout(String userId) {
        keycloakAdmin.realm(realm).users().get(userId).logout();
    }

    /**
     * Réinitialise le mot de passe d'un utilisateur
     */
    public void resetPassword(String userId, String newPassword) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(newPassword);
        credential.setTemporary(false);

        keycloakAdmin.realm(realm).users().get(userId).resetPassword(credential);
    }

    /**
     * Vérifie si un token JWT est valide et non expiré
     */
    public boolean validateToken(String token) {
        try {
            String tokenUrl = this.authServerUrl + "/realms/" + this.realm + "/protocol/openid-connect/userinfo";

            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tokenUrl))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

}
