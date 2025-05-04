package com.nutrition.API_nutrition.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutrition.API_nutrition.config.KeycloakProvider;
import com.nutrition.API_nutrition.exception.ApiException;
import com.nutrition.API_nutrition.exception.ErrorCode;
import com.nutrition.API_nutrition.model.dto.RegisterRequestDto;
import com.nutrition.API_nutrition.model.dto.TokenResponseDto;
import com.nutrition.API_nutrition.util.HttpClientConfig;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
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
        keycloakProvider.logKeycloakConfig();
        return this.keycloakProvider.getKeycloakInstance();
    }

    private String getRealm() {
        return this.keycloakProvider.getRealm();
    }

    private String getUrl() {
        return this.keycloakProvider.getAuthServerUrl();
    }

    /**
     * Crée un nouvel utilisateur dans Keycloak à partir des informations fournies dans le DTO {@link RegisterRequestDto}.
     *
     * <p>Cette méthode effectue les opérations suivantes :
     * <ul>
     *     <li>Construit une instance de {@link UserRepresentation} à partir des données du DTO.</li>
     *     <li>Recherche les utilisateurs existants dans Keycloak ayant le même nom d'utilisateur, prénom et nom.</li>
     *     <li>Si aucun utilisateur correspondant n'est trouvé, un nouvel utilisateur est créé dans Keycloak.</li>
     *     <li>Si la création réussit, l'identifiant Keycloak du nouvel utilisateur est extrait de l'en-tête de réponse
     *         et ajouté au DTO.</li>
     *     <li>Si l'utilisateur existe déjà, aucune création n'est effectuée et un message est journalisé.</li>
     * </ul>
     *
     * @param dto un objet {@link RegisterRequestDto} contenant les informations de l'utilisateur à créer.
     * @throws ApiException si une erreur survient lors de la création de l'utilisateur dans Keycloak.
     */
    public void createUser(RegisterRequestDto dto) {

        UserRepresentation user = getUserRepresentation(dto);

        // récupérer la liste des utilisateurs possèdent un nom spécifique
        List<UserRepresentation> listUser = getKc()
                .realm(getRealm())
                .users()
                .search(
                        user.getUsername(), user.getFirstName(), user.getLastName(),
                        null, null, null);

        this.displayList(listUser);

        // Si l'user n'existe pas
        if (listUser.isEmpty()) {

            Response response = getKc()
                    .realm(getRealm())
                    .users()
                    .create(user);

            String body = response.readEntity(String.class);
            log.info("Response body {}", body);

            if (response.getStatus() < 200 || response.getStatus() >= 300) {

                throw new ApiException(
                        "Failed to create user in Keycloak ",
                        HttpStatus.valueOf(response.getStatus()),
                        ErrorCode.USER_CREATION_FAILED.toString());
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
     * Assigne une liste de rôles Keycloak à un utilisateur spécifié par son identifiant.
     *
     * <p>Cette méthode effectue les étapes suivantes :
     * <ul>
     *     <li>Pour chaque nom de rôle fourni, récupère la représentation du rôle depuis le realm Keycloak.</li>
     *     <li>Journalise la liste des rôles récupérés.</li>
     *     <li>Assigne ces rôles au niveau du realm à l'utilisateur correspondant à l'identifiant fourni.</li>
     * </ul>
     *
     * <p>Elle gère plusieurs cas d'erreurs possibles :
     * <ul>
     *     <li><strong>WebApplicationException</strong> : Erreur retournée par Keycloak avec code HTTP, journalisée et transformée en {@link ApiException}.</li>
     *     <li><strong>ProcessingException</strong> : Erreur réseau ou de traitement local, transformée en {@link ApiException} avec statut 503.</li>
     *     <li><strong>Exception</strong> : Toute autre erreur inattendue, transformée en {@link ApiException} avec statut 500.</li>
     * </ul>
     *
     * @param userId    l'identifiant Keycloak de l'utilisateur auquel les rôles doivent être assignés.
     * @param roleNames la liste des noms des rôles à assigner.
     * @throws ApiException si une erreur survient durant l'opération d'assignation des rôles.
     */
    public void addUserRolesRealm(String userId, List<String> roleNames) throws ApiException {
        try {
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


            this.displayList(roles);

            // Assigner les rôles à l'utilisateur
            getKc().realm(getRealm())
                    .users()
                    .get(userId)
                    .roles()
                    .realmLevel()
                    .add(roles);

        } catch (WebApplicationException e) {

            int status = e.getResponse().getStatus();
            String body = e.getResponse().readEntity(String.class);
            log.error("HTTP Error {} : {}", status, "Keycloak replied with an error: " + body);

            throw new ApiException(
                    "An error occurred while assigning a role to the user '%s'".formatted(userId),
                    HttpStatus.valueOf(status),
                    ErrorCode.USER_ROLE_ASSIGNMENT_FAILED.toString()
            );

        } catch (ProcessingException e) {
            log.error("Network or local processing error : {}", e.getMessage());
            throw new ApiException(
                    "Network error during role assignment '%s'".formatted(userId),
                    HttpStatus.SERVICE_UNAVAILABLE,
                    ErrorCode.NETWORK_ERROR.toString()
            );

        } catch (Exception e) {
            log.error("An unexpected error has occurred : {}", e.getMessage(), e);
            throw new ApiException(
                    "Unknown error when assigning roles",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorCode.TECHNICAL_ERROR.toString()
            );
        }
    }

    public void addUserRolesClient(String userId, List<String> roleNames) throws ApiException {
        try {

            log.info("Get client API {} ", this.keycloakProvider.getClientId());
            List<ClientRepresentation> clients = getKc().realm(getRealm())
                    .clients()
                    .findByClientId(this.keycloakProvider.getClientId());

            Optional<ClientRepresentation> customerSearching = clients.stream()
                    .filter(client -> {
                        log.info("Client ID {} ", client.getClientId());
                        return client.getClientId().equals(this.keycloakProvider.getClientId());
                    })
                    .findFirst();


            if (customerSearching.isEmpty()) {
                throw new ApiException(
                        "an error occurred while searching for the API client '%s'".formatted(this.keycloakProvider.getClientId()),
                        HttpStatus.BAD_REQUEST,
                        ErrorCode.USER_ROLE_ASSIGNMENT_FAILED.toString()
                );
            }

            String clientIdApi = customerSearching.get().getClientId();
            log.info("Search role client in API customers {}", customerSearching.get().getClientId());


            List<RoleRepresentation> roles = roleNames.stream()
                    .map(roleName -> {
                        log.info("Role search {} in the customer {}", roleName, customerSearching.get().getClientId());
                        return getKc().realm(getRealm())
                                .clients()
                                .get(customerSearching.get().getId())
                                .roles()
                                .get(roleName)
                                .toRepresentation();
                    })
                    .collect(Collectors.toList());


            this.displayList(roles);

            // Assigner des rôles à un utilisateur pour un client spécifique
            getKc().realm(getRealm())
                    .users()
                    .get(userId)
                    .roles()
                    .clientLevel(customerSearching.get().getId())
                    .add(roles);

        } catch (WebApplicationException e) {

            int status = e.getResponse().getStatus();
            String body = e.getResponse().readEntity(String.class);
            log.error("HTTP Error {} : {}", status, "Keycloak replied with an error: " + body);

            throw new ApiException(
                    "An error occurred while assigning a role to the user '%s'".formatted(userId),
                    HttpStatus.valueOf(status),
                    ErrorCode.USER_ROLE_ASSIGNMENT_FAILED.toString()
            );

        } catch (ProcessingException e) {
            log.error("Network or local processing error : {}", e.getMessage());
            throw new ApiException(
                    "Network error during role assignment '%s'".formatted(userId),
                    HttpStatus.SERVICE_UNAVAILABLE,
                    ErrorCode.NETWORK_ERROR.toString()
            );

        } catch (Exception e) {
            log.error("An unexpected error has occurred : {}", e.getMessage(), e);
            throw new ApiException(
                    "Unknown error when assigning roles",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorCode.TECHNICAL_ERROR.toString()
            );
        }
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
        log.info("User Representation: {}", user);

        // Définir les credentials
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(dto.getPassword());
        credential.setTemporary(false);
        log.info("User credential: {}", credential);

        // add list credentials
        user.setCredentials(List.of(credential));

        return user;
    }

    /**
     * Recherche un user par son id dans Keycloak
     */
    public boolean userExistsById(String userId) {

        if (userId == null || userId.isEmpty()) {
            log.error("userId is null when calling userExistsById");
            return false;
        }

        try {
            UserRepresentation user = getKc()
                    .realm(getRealm())
                    .users()
                    .get(userId)
                    .toRepresentation();

            log.info("User is {}", user);
            return user != null;

        } catch (NotFoundException e) {
            log.error("Error user not Found {}", userId, e);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error {}", userId, e);
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

    /**
     * Permet de logger les listes
     *
     * @param ls
     * @param <T>
     */
    <T> void displayList(List<T> ls) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            log.info("Display list {}", mapper.writeValueAsString(ls));
        } catch (JsonProcessingException e) {
            log.error("Error serializing list", e);
        }
    }

}
