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
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
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
            dto.getKeycloakUserData().setKeycloakId(userId);


        } else {
            log.info("User already exists {}", user.getUsername());
        }

    }

    /**
     * Assigne une liste de rôles Keycloak à un utilisateur au **niveau du realm**.
     *
     * <p>Contrairement à l’assignation de rôles au niveau d’un client spécifique (client-level roles),
     * cette méthode affecte les rôles globaux du realm (realm-level roles), qui sont indépendants des permissions
     * d’un client particulier.
     *
     * <p>La méthode effectue les étapes suivantes :
     * <ul>
     *     <li>Pour chaque nom de rôle fourni, récupère sa représentation depuis les rôles du realm.</li>
     *     <li>Journalise les rôles récupérés pour traçabilité.</li>
     *     <li>Assigne ces rôles au niveau du realm à l’utilisateur correspondant à l’identifiant fourni.</li>
     * </ul>
     *
     * <p>Elle gère les cas d’erreurs suivants :
     * <ul>
     *     <li><strong>WebApplicationException</strong> : réponse d’erreur HTTP de Keycloak, journalisée et encapsulée dans une {@link ApiException}.</li>
     *     <li><strong>ProcessingException</strong> : problème réseau ou local, transformé en {@link ApiException} avec un statut 503.</li>
     *     <li><strong>Exception</strong> : toute erreur imprévue, encapsulée dans une {@link ApiException} avec un statut 500.</li>
     * </ul>
     *
     * @param userId              l’identifiant Keycloak de l’utilisateur à qui assigner les rôles.
     * @param roleRepresentations la liste des noms des rôles realm-level à assigner.
     * @throws ApiException si une erreur survient durant l’assignation des rôles.
     */
    public void addUserRolesRealm(String userId, List<RoleRepresentation> roleRepresentations) throws ApiException {

        try {

            // Assigner les rôles à l'utilisateur
            getKc().realm(getRealm())
                    .users()
                    .get(userId)
                    .roles()
                    .realmLevel()
                    .add(roleRepresentations);

        } catch (WebApplicationException e) {

            int status = e.getResponse().getStatus();
            String body = e.getResponse().readEntity(String.class);
            log.error("HTTP Error {} : {}", status, "Keycloak replied with an error: " + body);

            throw new ApiException(
                    "An error occurred while assigning a role to the user '%s'".formatted(userId),
                    HttpStatus.valueOf(status),
                    ErrorCode.USER_ROLE_ASSIGNMENT_FAILED.toString()
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
     * Assigne une liste de rôles Keycloak à un utilisateur au **niveau d’un client** spécifique.
     *
     * <p>Cette méthode permet d'affecter des rôles qui sont associés à un client particulier dans Keycloak,
     * ce qui signifie que ces rôles sont liés à une application ou un service précis.
     * Contrairement à l’assignation de rôles au niveau du realm, ces rôles sont spécifiques à un client (client-level roles).
     *
     * <p>La méthode effectue les étapes suivantes :
     * <ul>
     *     <li>Récupère le client correspondant à l'ID client spécifié dans la configuration Keycloak.</li>
     *     <li>Récupère la représentation de chaque rôle à assigner dans le client spécifié.</li>
     *     <li>Assigne ces rôles à l'utilisateur spécifié au niveau du client.</li>
     * </ul>
     *
     * <p>Elle gère les cas d’erreurs suivants :
     * <ul>
     *     <li><strong>WebApplicationException</strong> : réponse d’erreur HTTP de Keycloak, journalisée et encapsulée dans une {@link ApiException}.</li>
     *     <li><strong>ProcessingException</strong> : problème réseau ou local, transformé en {@link ApiException} avec un statut 503.</li>
     *     <li><strong>Exception</strong> : toute erreur imprévue, encapsulée dans une {@link ApiException} avec un statut 500.</li>
     * </ul>
     *
     * @param userId              l’identifiant Keycloak de l’utilisateur auquel les rôles doivent être assignés.
     * @param roleRepresentations la liste des noms des rôles client-level à assigner.
     * @throws ApiException si une erreur survient lors de l’assignation des rôles.
     */
    public void addUserRolesClient(String userId, List<RoleRepresentation> roleRepresentations) throws ApiException {

        try {

            if (roleRepresentations == null || roleRepresentations.isEmpty()) {
                throw new ApiException("No roles to assign",
                        HttpStatus.BAD_REQUEST,
                        ErrorCode.USER_ROLE_ASSIGNMENT_FAILED.toString());
            }

            List<ClientRepresentation> clients = getKc().realm(getRealm())
                    .clients()
                    .findByClientId(this.keycloakProvider.getClientId());

            if (clients.isEmpty()) {
                throw new ApiException("Client not found: " + this.keycloakProvider.getClientId(),
                        HttpStatus.BAD_REQUEST,
                        ErrorCode.USER_ROLE_ASSIGNMENT_FAILED.toString());
            }

            String clientUuid = clients.get(0).getId();
            log.debug("Client UUID retrieved for clientId '{}': {}", this.keycloakProvider.getClientId(), clientUuid);

            // Assigner des rôles à un utilisateur pour un client spécifique
            getKc().realm(getRealm())
                    .users()
                    .get(userId)
                    .roles()
                    .clientLevel(clientUuid)
                    .add(roleRepresentations);

        } catch (WebApplicationException e) {

            int status = e.getResponse().getStatus();
            String body = e.getResponse().readEntity(String.class);

            log.error("The role could not be assigned to the user : {}", e.getMessage(), e);
            log.error("HTTP Error {} : {}", status, "Keycloak replied with an error: " + body);

            throw new ApiException(
                    "An error occurred while assigning a role to the user '%s'".formatted(userId),
                    HttpStatus.valueOf(status),
                    ErrorCode.USER_ROLE_ASSIGNMENT_FAILED.toString()
            );

        } catch (ApiException e) {
            throw e;
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
     * get list role realm
     */
    public List<RoleRepresentation> getRealmScopedRoles() {
        return getKc().realm(getRealm())
                .roles()
                .list();
    }

    /**
     * get list role client
     */
    public List<RoleRepresentation> getClientScopedRoles() {
        ClientRepresentation client = getKc().realm(getRealm())
                .clients()
                .findByClientId(this.keycloakProvider.getClientId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new ApiException(
                        "Client not found",
                        HttpStatus.BAD_REQUEST,
                        ErrorCode.KEYCLOAK_BAD_REQUEST.toString()
                ));

        return getKc().realm(getRealm())
                .clients()
                .get(client.getId())
                .roles()
                .list();
    }

    /**
     * Delete role Realm
     *
     * @param userId
     * @param roleRepresentations
     */
    public void removeRealmRoleFromUser(String userId, List<RoleRepresentation> roleRepresentations) {

        getKc().realm(getRealm())
                .users()
                .get(userId)
                .roles()
                .realmLevel()
                .remove(roleRepresentations);
    }

    /**
     * Delete role client
     *
     * @param userId
     * @param roleRepresentations
     */
    public void removeClientRoleFromUser(String userId, List<RoleRepresentation> roleRepresentations) {

        getKc().realm(getRealm())
                .users()
                .get(userId)
                .roles()
                .clientLevel(userId)
                .remove(roleRepresentations);
    }


    /**
     * Authentifie un utilisateur auprès de Keycloak en utilisant le flux "password grant" de OpenID Connect.
     *
     * <p>Cette méthode envoie une requête de type POST au serveur Keycloak pour récupérer un token d'accès
     * et un token de rafraîchissement (refresh token) via l'authentification par nom d'utilisateur et mot de passe.
     * Elle utilise l'URL de token configurée pour le serveur Keycloak et inclut le client_id, client_secret,
     * ainsi que les informations d'identification de l'utilisateur dans le corps de la requête.
     *
     * <p>En cas de succès, elle retourne un {@link TokenResponseDto} contenant les tokens nécessaires pour
     * l'authentification.
     *
     * <p>Elle gère les erreurs suivantes :
     * <ul>
     *     <li><strong>RuntimeException</strong> : Si l'authentification échoue (réponse HTTP non 200), une exception
     *     est lancée.
     *     </li>
     *     <li><strong>Exception</strong> : Toute autre erreur imprévue pendant le processus d'authentification est
     *     capturée et lancée sous forme de {@link RuntimeException}.
     *     </li>
     * </ul>
     *
     * @param username le nom d'utilisateur pour l'authentification.
     * @param password le mot de passe de l'utilisateur.
     * @return un {@link TokenResponseDto} contenant l'access token, le refresh token et les durées d'expiration.
     * @throws RuntimeException si l'authentification échoue ou si une erreur survient pendant le processus.
     */
    public TokenResponseDto login(String username, String password) throws ApiException {

        try {

            String tokenUrl = getUrl() + "/realms/" + getRealm() + "/protocol/openid-connect/token";
            String header = "application/x-www-form-urlencoded";
            String body = "client_id=" + this.keycloakProvider.getClientId() +
                    "&client_secret=" + this.keycloakProvider.getClientSecret() +
                    "&grant_type=password" +
                    "&username=" + URLEncoder.encode(username, StandardCharsets.UTF_8) +
                    "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8) +
                    "&scope=openid";

            log.info("Token Url : {}", tokenUrl);
            HttpRequest request = this.httpClientConfig.postRequest(tokenUrl, header, body);

            // send to keycloak
            HttpResponse<String> response = this.httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                log.error("Authentication failed for user '{}'. Status: {}, Response: {}",
                        username, response.statusCode(), response.body());

                String errorMessage = " Authentication failed with Keycloak.";
                switch (response.statusCode()) {
                    case 400 -> throw new ApiException(
                            errorMessage,
                            HttpStatus.BAD_REQUEST,
                            ErrorCode.KEYCLOAK_BAD_REQUEST.toString()
                    );
                    case 401 -> throw new ApiException(
                            "Invalid credentials." + errorMessage,
                            HttpStatus.UNAUTHORIZED,
                            ErrorCode.KEYCLOAK_UNAUTHORIZED.toString()
                    );
                    case 403 -> throw new ApiException(
                            "Access forbidden." + errorMessage,
                            HttpStatus.FORBIDDEN,
                            ErrorCode.KEYCLOAK_FORBIDDEN.toString()
                    );
                    case 404 -> throw new ApiException(
                            "User not exist." + errorMessage,
                            HttpStatus.FORBIDDEN,
                            ErrorCode.KEYCLOAK_FORBIDDEN.toString()
                    );
                    default -> throw new ApiException(
                            "Unexpected error during authentication. " + errorMessage,
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            ErrorCode.KEYCLOAK_UNEXPECTED_ERROR.toString()
                    );
                }
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

        } catch (ApiException e) {
            // Si c’est une ApiException lancée volontairement, on la relance telle quelle
            throw e;
        } catch (Exception e) {
            log.error("Technical error during authentication request", e);
            throw new ApiException(
                    "A technical error has occurred during the authentication request",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorCode.TECHNICAL_ERROR.toString()
            );
        }
    }

    /**
     * Déconnecte un utilisateur de Keycloak en utilisant son identifiant.
     *
     * <p>Cette méthode tente de fermer la session de l'utilisateur spécifié par son identifiant
     * Keycloak. Si la déconnexion est réussie, la méthode retourne {@code true}.
     * Si une erreur se produit pendant la déconnexion, elle retourne {@code false}.
     *
     * <p>Les erreurs possibles incluent :
     * <ul>
     *     <li><strong>Exception</strong> : Toute erreur imprévue durant la déconnexion de l'utilisateur est capturée et journalisée.</li>
     * </ul>
     *
     * @param userId l'identifiant Keycloak de l'utilisateur à déconnecter.
     */
    public void logout(String userId) {

        try {
            getKc().realm(getRealm())
                    .users()
                    .get(userId)
                    .logout();

            log.info("Logout successfully closed {}", userId);


        } catch (ForbiddenException e) {
            // Cas spécifique
            throw new ApiException("Access forbidden",
                    HttpStatus.FORBIDDEN,
                    ErrorCode.KEYCLOAK_FORBIDDEN.toString());

        } catch (WebApplicationException ex) {
            // Cas généraux

            Response body = ex.getResponse();
            int status = body.getStatus();

            log.error("Logout failed. Status: {}, Entity: {}", status, body.readEntity(String.class));

            switch (status) {
                case 400:
                    throw new ApiException("Requête mal formée : " + body,
                            HttpStatus.BAD_REQUEST,
                            ErrorCode.KEYCLOAK_BAD_REQUEST.toString());
                case 401:
                    throw new ApiException("Non autorisé : " + body,
                            HttpStatus.UNAUTHORIZED,
                            ErrorCode.KEYCLOAK_UNAUTHORIZED.toString());
                case 403:
                    throw new ApiException("Accès interdit : " + body,
                            HttpStatus.FORBIDDEN,
                            ErrorCode.KEYCLOAK_FORBIDDEN.toString());
                case 404:
                    throw new ApiException("Utilisateur non trouvé : " + body,
                            HttpStatus.NOT_FOUND,
                            ErrorCode.KEYCLOAK_USER_NOT_FOUND.toString());
                default:
                    throw new ApiException("Erreur inattendue : " + body,
                            HttpStatus.valueOf(status),
                            ErrorCode.KEYCLOAK_UNEXPECTED_ERROR.toString());
            }

        } catch (Exception e) {
            throw new ApiException("Technical error during disconnection",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorCode.TECHNICAL_ERROR.toString());
        }
    }

    /**
     * Rafraîchit un token d'accès Keycloak à l'aide du refresh token.
     *
     * <p>Cette méthode envoie une requête de type POST au serveur Keycloak pour obtenir un nouveau token d'accès
     * en utilisant un refresh token valide. Elle utilise l'URL de token configurée pour le serveur Keycloak et
     * inclut le client_id, client_secret, ainsi que le refresh token dans le corps de la requête.
     *
     * <p>En cas de succès, elle retourne un {@link TokenResponseDto} contenant le nouveau access token,
     * le refresh token, ainsi que les durées d'expiration associées.
     *
     * <p>Elle gère les erreurs suivantes :
     * <ul>
     *     <li><strong>RuntimeException</strong> : Si le rafraîchissement du token échoue (réponse HTTP non 200), une exception est lancée avec le message d'erreur.</li>
     *     <li><strong>Exception</strong> : Toute autre erreur imprévue lors du processus de rafraîchissement du token est capturée et lancée sous forme de {@link RuntimeException}.</li>
     * </ul>
     *
     * @param refreshToken le refresh token utilisé pour obtenir un nouveau access token.
     * @return un {@link TokenResponseDto} contenant le nouveau access token, le refresh token, ainsi que les durées d'expiration.
     * @throws RuntimeException si le rafraîchissement du token échoue ou si une erreur survient pendant le processus.
     */
    public TokenResponseDto refreshToken(String refreshToken) {
        try {

            String tokenUrl = getUrl() + "/realms/" + getRealm() + "/protocol/openid-connect/token";
            String header = "application/x-www-form-urlencoded";
            String body = "client_id=" + this.keycloakProvider.getClientId() +
                    "&client_secret=" + this.keycloakProvider.getClientSecret() +
                    "&grant_type=refresh_token" +
                    "&refresh_token=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);

            // create request
            HttpRequest request = this.httpClientConfig.postRequest(tokenUrl, header, body);

            // send to keycloak
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Refresh token failed Status: {}, Response: {}",
                        response.statusCode(), response.body());

                String errorMessage = "Refresh token failed";
                switch (response.statusCode()) {
                    case 400 -> throw new ApiException(
                            errorMessage,
                            HttpStatus.BAD_REQUEST,
                            ErrorCode.KEYCLOAK_BAD_REQUEST.toString()
                    );
                    case 401 -> throw new ApiException(
                            "Invalid credentials. " + errorMessage,
                            HttpStatus.UNAUTHORIZED,
                            ErrorCode.KEYCLOAK_UNAUTHORIZED.toString()
                    );
                    case 403 -> throw new ApiException(
                            "Access forbidden. " + errorMessage,
                            HttpStatus.FORBIDDEN,
                            ErrorCode.KEYCLOAK_FORBIDDEN.toString()
                    );
                    default -> throw new ApiException(
                            "Unexpected error during authentication. " + errorMessage,
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            ErrorCode.KEYCLOAK_UNEXPECTED_ERROR.toString()
                    );
                }
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(response.body());

            return new TokenResponseDto(
                    node.get("access_token").asText(),
                    node.get("refresh_token").asText(),
                    node.get("expires_in").asLong(),
                    node.get("refresh_expires_in").asLong()
            );
        } catch (ApiException e) {
            throw e;
        }catch (Exception e) {
            throw new ApiException(
                    "A technical error has occurred during the token refresh process",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorCode.TECHNICAL_ERROR.toString()
            );
        }
    }


    /**
     * Crée une représentation d'utilisateur à partir des informations fournies dans un objet {@link RegisterRequestDto}.
     *
     * <p>Cette méthode crée un objet {@link UserRepresentation} qui est utilisé pour représenter un utilisateur
     * dans Keycloak. Elle définit les propriétés de l'utilisateur telles que le nom d'utilisateur, l'email, le prénom,
     * et le nom de famille. Elle assigne également un mot de passe à l'utilisateur en créant un objet {@link CredentialRepresentation}.
     *
     * <p>Les étapes suivantes sont effectuées :
     * <ul>
     *     <li>Initialisation des informations de base de l'utilisateur (nom, email, prénom, nom de famille).</li>
     *     <li>Création des informations d'authentification utilisateur (mot de passe).</li>
     *     <li>Retour de l'objet {@link UserRepresentation} avec les informations nécessaires à l'enregistrement dans Keycloak.</li>
     * </ul>
     *
     * @param dto l'objet contenant les informations de l'utilisateur à créer (nom, email, prénom, mot de passe, etc.).
     * @return un objet {@link UserRepresentation} contenant les informations de l'utilisateur à enregistrer.
     */
    private static UserRepresentation getUserRepresentation(RegisterRequestDto dto) {

        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername(dto.getKeycloakUserData().getUserName());
        user.setEmail(dto.getKeycloakUserData().getEmail());
        user.setFirstName(dto.getKeycloakUserData().getFirstName());
        user.setLastName(dto.getKeycloakUserData().getLastName());
        log.info("User Representation: {}", user);

        // Définir les credentials
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(dto.getKeycloakUserData().getPassword());
        credential.setTemporary(false);
        log.info("User credential: {}", credential);

        // add list credentials
        user.setCredentials(List.of(credential));

        return user;
    }

    /**
     * Vérifie si un utilisateur existe dans Keycloak en fonction de l'identifiant Keycloak ou des informations du DTO.
     *
     * <p>Cette méthode cherche à déterminer si un utilisateur avec les informations fournies existe dans Keycloak.
     * Elle effectue une recherche selon les critères suivants :
     * <ul>
     *     <li>Si l'identifiant Keycloak est fourni, elle recherche l'utilisateur directement par cet ID.</li>
     *     <li>Sinon, elle effectue une recherche par nom d'utilisateur, prénom, nom de famille et email, en utilisant les informations contenues dans le DTO.</li>
     * </ul>
     *
     * <p>En cas de succès, elle retourne {@code true} si un utilisateur correspondant aux critères est trouvé.
     * Sinon, elle retourne {@code false}. En cas d'erreur (utilisateur non trouvé ou exception imprévue), la méthode
     * retourne également {@code false}.
     *
     * <p>Les erreurs gérées incluent :
     * <ul>
     *     <li><strong>NotFoundException</strong> : Si l'utilisateur recherché par son identifiant Keycloak est introuvable, l'exception est capturée et {@code false} est retourné.</li>
     *     <li><strong>Exception</strong> : Toute autre exception imprévue durant le processus de recherche est capturée, et {@code false} est retourné.</li>
     * </ul>
     *
     * @param dto les informations de l'utilisateur à vérifier (nom, prénom, email, ID Keycloak).
     * @return {@code true} si un utilisateur correspondant est trouvé, {@code false} sinon.
     */
    public boolean checkUserExist(RegisterRequestDto dto) {

        try {

            if (dto.getKeycloakUserData().getKeycloakId() != null && !dto.getKeycloakUserData().getKeycloakId().isEmpty()) {

                log.info("Searching user by keycloak ");
                UserRepresentation user = getKc()
                        .realm(getRealm())
                        .users()
                        .get(dto.getKeycloakUserData().getKeycloakId())
                        .toRepresentation(); // → Lance NotFoundException si l'ID est introuvable
                log.info("User is {}", user);

                return user.getUsername().equals(dto.getKeycloakUserData().getUserName()) && user.getEmail().equals(dto.getKeycloakUserData().getEmail());

            } else {

                log.info("Searching user by dto Object ");
                // récupérer la liste des utilisateurs possèdent un nom spécifique
                List<UserRepresentation> listUser = getKc()
                        .realm(getRealm())
                        .users()
                        .search(
                                dto.getKeycloakUserData().getUserName(),
                                dto.getKeycloakUserData().getFirstName(),
                                dto.getKeycloakUserData().getLastName(),
                                dto.getKeycloakUserData().getEmail(), null, null);

                if (listUser.isEmpty()) {
                    log.info("No user found for the search criteria.");
                } else {
                    log.info("User first list {}", listUser.get(0));
                    this.displayList(listUser);
                }


                Optional<UserRepresentation> lsUserRep = listUser.stream()
                        .filter(userRepresentation -> {
                            return userRepresentation.getUsername()
                                    .equals(
                                            dto.getKeycloakUserData().getUserName()) &&
                                    userRepresentation.getEmail().equals(dto.getKeycloakUserData().getEmail()
                                    );
                        }).findFirst();

                return lsUserRep.isPresent();

            }

        } catch (NotFoundException e) {
            log.error("Error user not Found, ID Keycloak: {}", dto.getKeycloakUserData().getKeycloakId(), e);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error, ID Keycloak: {}", dto.getKeycloakUserData().getKeycloakId(), e);
            return false;
        }

    }

    /**
     * Met à jour les informations d'un utilisateur existant dans Keycloak.
     *
     * <p>Cette méthode effectue les étapes suivantes pour mettre à jour les informations d'un utilisateur dans Keycloak :
     * <ul>
     *     <li>Récupère l'utilisateur à partir de son identifiant Keycloak.</li>
     *     <li>Met à jour le nom d'utilisateur, prénom, nom de famille et adresse e-mail avec les informations du DTO.</li>
     *     <li>Si un mot de passe est fourni dans le DTO, il sera utilisé pour la mise à jour. Si aucun mot de passe n'est fourni, une exception est levée.</li>
     *     <li>Effectue la mise à jour de l'utilisateur dans Keycloak.</li>
     * </ul>
     *
     * <p>La méthode retourne {@code true} si la mise à jour de l'utilisateur a été effectuée avec succès.
     * Si une erreur se produit, comme un utilisateur introuvable ou un mot de passe manquant, elle retourne {@code false}.
     *
     * <p>Les erreurs gérées incluent :
     * <ul>
     *     <li><strong>RuntimeException</strong> : Si aucun mot de passe n'est fourni, une exception est levée.</li>
     *     <li><strong>Exception</strong> : Toute autre exception imprévue durant la mise à jour de l'utilisateur est capturée et journalisée.</li>
     * </ul>
     *
     * @param dto les informations de l'utilisateur à mettre à jour (nom d'utilisateur, prénom, nom de famille, e-mail, et mot de passe).
     * @return {@code true} si la mise à jour de l'utilisateur a été effectuée avec succès, {@code false} en cas d'erreur.
     */
    public boolean updateUser(RegisterRequestDto dto) {

        try {

            // Vérifier si l'utilisateur existe avant de tenter la mise à jour
            UserResource userResource = getKc()
                    .realm(getRealm())
                    .users()
                    .get(dto.getKeycloakUserData().getKeycloakId());

            // Cette ligne lancera une exception si l'utilisateur n'existe pas
            UserRepresentation existingUser = userResource.toRepresentation();
            existingUser.setUsername(dto.getKeycloakUserData().getUserName());
            existingUser.setFirstName(dto.getKeycloakUserData().getFirstName());
            existingUser.setLastName(dto.getKeycloakUserData().getLastName());
            existingUser.setEmail(dto.getKeycloakUserData().getEmail());

            // Mise à jour du mot de passe
            if (dto.getKeycloakUserData().getPassword() == null || dto.getKeycloakUserData().getPassword().isEmpty()) {
                throw new RuntimeException("The user's password is missing!");
            }

            userResource.update(existingUser);

            log.info("User update {}", dto.getKeycloakUserData().getUserName());
            return true;

        } catch (Exception e) {
            log.error("User update error {}", dto.getKeycloakUserData().getUserName(), e);
            return false;
        }
    }

    /**
     * Supprime un utilisateur de Keycloak en utilisant son identifiant.
     *
     * <p>Cette méthode tente de supprimer un utilisateur à partir de son identifiant Keycloak.
     * Si l'utilisateur est trouvé et supprimé avec succès, la méthode retourne {@code true}.
     * Si une erreur se produit pendant la suppression, elle retourne {@code false}.
     *
     * <p>Les erreurs possibles incluent :
     * <ul>
     *     <li><strong>Exception</strong> : Toute erreur imprévue durant la suppression de l'utilisateur est capturée et journalisée.</li>
     * </ul>
     *
     * @param userId l'identifiant Keycloak de l'utilisateur à supprimer.
     * @return {@code true} si l'utilisateur a été supprimé avec succès, {@code false} en cas d'erreur.
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
     * Réinitialise le mot de passe d'un utilisateur dans Keycloak.
     *
     * <p>Cette méthode permet de modifier le mot de passe d'un utilisateur existant en utilisant son identifiant {@code userId}
     * et un nouveau mot de passe {@code newPassword}. Le mot de passe est défini comme non temporaire.
     *
     * <p>Les étapes suivantes sont effectuées :
     * <ul>
     *     <li>Création d'un objet {@link CredentialRepresentation} avec le type "password" et la valeur du nouveau mot de passe.</li>
     *     <li>Appel de l'API Keycloak pour réinitialiser le mot de passe de l'utilisateur spécifié.</li>
     * </ul>
     *
     * @param userId      l'identifiant de l'utilisateur dont le mot de passe doit être réinitialisé.
     * @param newPassword le nouveau mot de passe de l'utilisateur.
     * @throws RuntimeException si une erreur se produit lors de la réinitialisation du mot de passe.
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
     * Affiche une liste d'objets sous forme de chaîne JSON dans les logs.
     *
     * <p>Cette méthode prend une liste d'objets génériques {@code ls}, puis sérialise cette liste en chaîne JSON
     * à l'aide de l'objet {@link ObjectMapper} de Jackson. La chaîne résultante est ensuite affichée dans les logs
     * à l'aide du niveau {@code INFO}. Si une erreur survient lors de la sérialisation, un message d'erreur est enregistré
     * dans les logs.
     *
     * @param ls  la liste des objets à afficher sous forme JSON dans les logs.
     * @param <T> le type des objets dans la liste.
     */
    <T> void displayList(List<T> ls) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            log.info("Display list {}", mapper.writeValueAsString(ls));
        } catch (JsonProcessingException e) {
            log.error("Error serializing list", e);
        }
    }

    public List<UserRepresentation> getAllUser() {
        return this.getKc().realm(this.getRealm()).users().list();
    }


}
