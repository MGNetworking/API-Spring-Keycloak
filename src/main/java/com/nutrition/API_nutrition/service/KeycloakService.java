package com.nutrition.API_nutrition.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutrition.API_nutrition.config.KeycloakProvider;
import com.nutrition.API_nutrition.model.dto.RegisterRequestDto;
import com.nutrition.API_nutrition.model.dto.TokenResponseDto;
import com.nutrition.API_nutrition.util.HttpClientConfig;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service responsable de la gestion des utilisateurs dans Keycloak.
 * Fournit des méthodes pour créer, modifier, supprimer des utilisateurs
 * et gérer leurs rôles (realm et client).
 */
@Slf4j
@Service
public class KeycloakService {

    private final KeycloakProvider keycloakProvider;
    private final HttpClient httpClient;
    private final HttpClientConfig httpClientConfig;

    public KeycloakService(KeycloakProvider keycloakProvider, HttpClient httpClient, HttpClientConfig httpClientConfig) {
        this.keycloakProvider = keycloakProvider;
        this.httpClient = httpClient;
        this.httpClientConfig = httpClientConfig;
    }

    private Keycloak getKc() {
        return this.keycloakProvider.getKeycloakInstance();
    }

    private String getRealm() {
        return this.keycloakProvider.getRealm();
    }

    private String getUrl() {
        return this.keycloakProvider.getAuthServerUrl();
    }

    /**
     * Crée un nouvel utilisateur dans Keycloak est ajout l'ID au DTO
     */
    public void createUser(RegisterRequestDto dto) {

        UserRepresentation user = getUserRepresentation(dto);

        // récupérer la liste des utilisateurs possèdent un nom spécifique
        List<UserRepresentation> users = getKc()
                .realm(getRealm())
                .users()
                .search(user.getUsername(), true);

        // Si l'user n'existe pas
        if (users.isEmpty()) {

            Response response = getKc()
                    .realm(getRealm())
                    .users()
                    .create(user);

            if (response.getStatus() < 200 || response.getStatus() >= 300) {
                throw new RuntimeException("Failed to create user in Keycloak: " + response.getStatusInfo());
            }

            // Extraire l'ID du nouvel utilisateur
            String locationPath = response.getLocation().getPath();
            String userId = locationPath.substring(locationPath.lastIndexOf('/') + 1);
            log.info("user Id extract : {}", userId);

            // add keycloak id
            dto.setKeycloakId(userId);


        } else {
            log.info("User already exists {}", user.getUsername());
        }

    }

    /**
     * Ajoute des rôles à un utilisateur
     */
    public void addUserRoles(String userId, List<String> roleNames) {

        log.info("Search for and retrieve the roles to be signed by the target user.");
        List<RoleRepresentation> roles = roleNames.stream()
                .map(roleName -> {
                    log.info("Check that the following role exists in realm {}", roleName);
                    return getKc().realm(getRealm())
                            .roles()
                            .get(roleName)
                            .toRepresentation();
                })
                .collect(Collectors.toList());

        try {
            ObjectMapper mapper = new ObjectMapper();
            log.info("User signature list {}", mapper.writeValueAsString(roles));
        } catch (JsonProcessingException e) {
            log.error("Parsing error for role display ");
        }

        // Assigner les rôles à l'utilisateur
        getKc().realm(getRealm())
                .users()
                .get(userId)
                .roles()
                .realmLevel()
                .add(roles);
    }

    /**
     * Authentifie un utilisateur et retourne les tokens
     * Peut être utilisée comment accès token
     */
    public TokenResponseDto login(String username, String password) {

        try {

            String tokenUrl = getUrl() + "/realms/" + getRealm() + "/protocol/openid-connect/token";
            String header = "application/x-www-form-urlencoded";
            String body = "client_id=" + this.keycloakProvider.getClientId() +
                    "&client_secret=" + this.keycloakProvider.getClientSecret() +
                    "&grant_type=password" +
                    "&username=" + URLEncoder.encode(username, StandardCharsets.UTF_8) +
                    "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8);

            log.info("Token Url : {}", tokenUrl);
            HttpRequest request = this.httpClientConfig.postRequest(tokenUrl, header, body);

            // send to keycloak
            HttpResponse<String> response = this.httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                log.error("Authentication failed for this user {} and password: {}", username, password);
                throw new RuntimeException("Authentication failed: " + response.body());
            }

            // mapping to JSON response
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

            String tokenUrl = getUrl() + "/realms/" + getRealm() + "/protocol/openid-connect/token";
            String header = "application/x-www-form-urlencoded";
            String body = "client_id=" + this.keycloakProvider.getClientId() +
                    "&client_secret=" + this.keycloakProvider.getClientSecret() +
                    "&grant_type=refresh_token" +
                    "&refresh_token=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);

            HttpRequest request = this.httpClientConfig.postRequest(tokenUrl, header, body);
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
     * Vérifie si un token JWT est valide et non expiré
     */
    public boolean validateToken(String token) {
        try {

            String tokenUrl = this.keycloakProvider.getAuthServerUrl() +
                    "/realms/" +
                    this.keycloakProvider.getRealm() +
                    "/protocol/openid-connect/userinfo";

            String header = "Bearer " + token;
            HttpRequest request = this.httpClientConfig.getRequest(tokenUrl, header);
            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }


    /**
     * Création d'un objet de représentation pour l'authentification,
     * permettent de faire un mapping du DTO
     *
     * @param dto RegisterRequestDto
     * @return UserRepresentation
     */
    private static UserRepresentation getUserRepresentation(RegisterRequestDto dto) {
        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername(dto.getUserName());
        user.setEmail(dto.getEmail());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());

        // Définir les credentials
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(dto.getPassword());
        credential.setTemporary(false);

        // add list credentials
        user.setCredentials(List.of(credential));
        return user;
    }

    /**
     * Recherche un user par son id dans Keycloak
     */
    public boolean userExistsById(String userId) {

        try {
            UserRepresentation user = getKc()
                    .realm(getRealm())
                    .users()
                    .get(userId)
                    .toRepresentation();

            return user != null;

        } catch (Exception e) {
            log.error("Error verifying user existence {}", userId, e);
            return false;
        }

    }

    /**
     * Met à jour les informations d'un utilisateur dans le realm cibler
     *
     * @param dto RegisterRequestDto
     * @return boolean status
     */
    public boolean updateUser(RegisterRequestDto dto) {

        try {

            // Vérifier si l'utilisateur existe avant de tenter la mise à jour
            UserResource userResource = getKc()
                    .realm(getRealm())
                    .users()
                    .get(dto.getKeycloakId());

            // Cette ligne lancera une exception si l'utilisateur n'existe pas
            UserRepresentation existingUser = userResource.toRepresentation();
            existingUser.setUsername(dto.getUserName());
            existingUser.setFirstName(dto.getFirstName());
            existingUser.setLastName(dto.getLastName());
            existingUser.setEmail(dto.getEmail());

            // Mise à jour du mot de passe
            if (dto.getPassword() == null || dto.getPassword().isEmpty()) {
                throw new RuntimeException("The user's password is missing!");
            }

            userResource.update(existingUser);

            log.info("User update {}", dto.getUserName());
            return true;

        } catch (Exception e) {
            log.error("User update error {}", dto.getUserName(), e);
            return false;
        }
    }

    /**
     * delete un user dans le realm cibler
     *
     * @param userId String
     */
    public boolean removeUser(String userId) {

        try {
            getKc().realm(getRealm())
                    .users()
                    .get(userId)
                    .remove();

            log.info("Id User is delete successfully {}", userId);
            return true;

        } catch (Exception e) {

            log.error("Error when deleting a user");
            return false;
        }
    }

    /**
     * Déconnecte un utilisateur
     */
    public boolean logout(String userId) {

        try {
            getKc().realm(getRealm())
                    .users()
                    .get(userId)
                    .logout();

            log.info("Login successfully closed {}", userId);
            return true;

        } catch (Exception e) {

            log.error("Error closing connection {}", userId);
            return false;
        }
    }

    /**
     * Réinitialise le mot de passe d'un utilisateur
     */
    public void resetPassword(String userId, String newPassword) {

        try {

            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(newPassword);
            credential.setTemporary(false);

            getKc().realm(getRealm())
                    .users()
                    .get(userId)
                    .resetPassword(credential);

        } catch (Exception e) {
            log.error("Error modifying your user's password");
            throw new RuntimeException("Error modifying user password");
        }
    }

}
