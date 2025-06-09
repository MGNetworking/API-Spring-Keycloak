package com.nutrition.API_nutrition.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutrition.API_nutrition.config.KeycloakProvider;
import com.nutrition.API_nutrition.exception.ApiException;
import com.nutrition.API_nutrition.exception.ErrorCode;
import com.nutrition.API_nutrition.util.HttpClientConfig;
import jakarta.ws.rs.WebApplicationException;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.http.HttpClient;
import java.util.List;

/**
 * Service responsable de la gestion des utilisateurs dans Keycloak.
 * Fournit des méthodes pour créer, modifier, supprimer des utilisateurs
 * et gérer leurs rôles (realm et client).
 */
@Slf4j
@Service
public class KeycloakService {

    private final KeycloakProvider keycloakProvider;

    public KeycloakService(KeycloakProvider keycloakProvider) {
        this.keycloakProvider = keycloakProvider;
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
