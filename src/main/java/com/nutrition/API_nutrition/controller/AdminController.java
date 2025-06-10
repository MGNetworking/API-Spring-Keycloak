package com.nutrition.API_nutrition.controller;

import com.nutrition.API_nutrition.model.dto.UserWithRolesDto;
import com.nutrition.API_nutrition.model.response.GenericApiErrorResponse;
import com.nutrition.API_nutrition.model.response.GenericApiResponse;
import com.nutrition.API_nutrition.service.KeycloakService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping(AdminController.BASE_ADMIN)
@Tag(
        name = "Administration",
        description = "Endpoints réservés aux administrateurs pour la gestion des utilisateurs."
)
@RequiredArgsConstructor
public class AdminController {

    public final static String BASE_ADMIN = "/api/v1/admin";

    // Récupération des rôles
    public static final String GET_REALM_ROLES = "/roles/realm";
    public static final String GET_CLIENT_ROLES = "/roles/client/{targetClient}";

    // liste des utilisateurs et de leur role
    public static final String GET_ALL_USERS = "/users";
    public static final String GET_LIST_ROLE_CLIENT = "/user/{userId}/{targetClient}";

    // gestion des roles utilisateur sur domaine client
    public static final String ADD_USER_ROLE_CLIENT = "/user/{userId}/roles/client/{targetClient}";
    public static final String REMOVE_USER_ROLE_CLIENT = "/user/{userId}/roles/client/{targetClient}";

    // gestion des roles utilisateur sur domaine uniquement
    public static final String ADD_REALM_ROLE_TO_USER = "/users/{userId}/roles/realm";
    public static final String REMOVE_REALM_ROLE_FROM_USER = "/users/{userId}/roles/realm";

    private final KeycloakService keycloakService;

    @Tag(name = "Rôles Realm",
            description = "Opérations liées à la récupération des rôles disponibles dans le realm"
    )
    @Operation(
            summary = "Récupérer la liste des rôles disponibles dans le realm",
            description = "Permet de récupérer tous les rôles disponibles dans le realm Keycloak configuré"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Liste des rôles récupérée avec succès",
                    content = @Content(schema = @Schema(implementation = GenericApiResponse.class))),
            @ApiResponse(responseCode = "401",
                    description = "Non autorisé (token expiré ou manquant)",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "403",
                    description = "Accès interdit",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "500",
                    description = "Erreur technique inattendue",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
    })
    @GetMapping(value = GET_REALM_ROLES)
    public ResponseEntity<GenericApiResponse<List<RoleRepresentation>>> getListRolesRealm() {

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new GenericApiResponse<List<RoleRepresentation>>(
                        HttpStatus.OK,
                        HttpStatus.OK.value(),
                        "List of roles successfully retrieved",
                        BASE_ADMIN + GET_REALM_ROLES,
                        this.keycloakService.getRealmScopedRoles()
                ));

    }

    @Tag(name = "Rôles client",
            description = "Opérations liées à la récupération des rôles d’un client Keycloak")
    @Operation(
            summary = "Récupérer la liste des rôles du client Keycloak",
            description = "Retourne tous les rôles associés à un client spécifique dans Keycloak"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Liste des rôles du client récupérée avec succès",
                    content = @Content(schema = @Schema(implementation = GenericApiResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "Client introuvable",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "401",
                    description = "Non autorisé (token expiré ou manquant)",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "403",
                    description = "Accès interdit",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "500",
                    description = "Erreur technique inattendue",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class)))
    })
    @GetMapping(value = GET_CLIENT_ROLES)
    public ResponseEntity<GenericApiResponse<List<RoleRepresentation>>> getListRolesClient(
            @PathVariable String targetClient
    ) {

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new GenericApiResponse<List<RoleRepresentation>>(
                        HttpStatus.OK,
                        HttpStatus.OK.value(),
                        "The user was Successfully create",
                        BASE_ADMIN + GET_CLIENT_ROLES,
                        this.keycloakService.getClientScopedRoles(targetClient)
                ));

    }

    @Tag(name = "Liste des utilisateurs",
            description = "Opérations liées à la récupération de la liste des utilisateurs disponibles dans le client"
    )
    @Operation(
            summary = "Récupérer la liste des utilisateurs disponibles dans le client cible",
            description = "Permet de récupérer la liste des utilisateurs disponibles dans le client cible"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Liste des utilisateurs récupérée avec succès",
                    content = @Content(schema = @Schema(implementation = GenericApiResponse.class))),
            @ApiResponse(responseCode = "401",
                    description = "Non autorisé (token expiré ou manquant)",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "403",
                    description = "Accès interdit",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "500",
                    description = "Erreur technique inattendue",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
    })
    @GetMapping(value = GET_ALL_USERS)
    public ResponseEntity<GenericApiResponse<List<UserRepresentation>>> getListUserClient() {

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new GenericApiResponse<List<UserRepresentation>>(
                        HttpStatus.OK,
                        HttpStatus.OK.value(),
                        "List of roles successfully retrieved",
                        BASE_ADMIN + GET_REALM_ROLES,
                        this.keycloakService.getListRoleClient()
                ));

    }

    @Tag(name = "Liste des roles utilisateurs",
            description = "Opérations liées à la récupération de la liste des roles utilisateurs " +
                    "disponibles sur le client cible"
    )
    @Operation(
            summary = "Récupérer la liste des roles utilisateurs disponibles  sur le client cible",
            description = "Permet de récupérer la liste des roles des utilisateurs disponibles  sur le client cible"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Liste des roles utilisateurs récupérée avec succès",
                    content = @Content(schema = @Schema(implementation = GenericApiResponse.class))),
            @ApiResponse(responseCode = "401",
                    description = "Non autorisé (token expiré ou manquant)",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "403",
                    description = "Accès interdit",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "500",
                    description = "Erreur technique inattendue",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
    })
    @GetMapping(value = GET_LIST_ROLE_CLIENT)
    public ResponseEntity<GenericApiResponse<UserWithRolesDto>> getUserClient(
            @PathVariable String userId,
            @PathVariable String targetClient
    ) {

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new GenericApiResponse<UserWithRolesDto>(
                        HttpStatus.OK,
                        HttpStatus.OK.value(),
                        "List of roles successfully retrieved",
                        BASE_ADMIN + GET_REALM_ROLES,
                        this.keycloakService.getUserWithRoles(userId, targetClient)
                ));

    }


    @Tag(name = "Gestion des rôles",
            description = "Opérations d'ajout de rôles à un utilisateur dans le realm")
    @Operation(
            summary = "Ajouter des rôles à un utilisateur",
            description = "Attribue un ou plusieurs rôles de realm à un utilisateur dans Keycloak"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Rôles ajoutés avec succès",
                    content = @Content(schema = @Schema(implementation = GenericApiResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "Requête invalide (liste de rôles vide ou non conforme)",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "401",
                    description = "Non autorisé (token expiré ou manquant)",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "403",
                    description = "Accès interdit",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "404",
                    description = "Utilisateur introuvable",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "500",
                    description = "Erreur technique inattendue",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class)))
    })
    @PostMapping(value = ADD_REALM_ROLE_TO_USER)
    @PreAuthorize("@accessKeycloak.hasAccessToUser(#userDto.keycloakId)")
    public ResponseEntity<GenericApiResponse<Void>> addUserRolesRealm(
            @PathVariable String userId,
            @RequestBody List<RoleRepresentation> roleRepresentations
    ) {

        Optional<ResponseEntity<GenericApiResponse<Void>>> validationResponse = this.validateUserIdAndRoles(
                userId,
                roleRepresentations);

        if (validationResponse.isPresent()) {
            return validationResponse.get();
        }

        // Ajout des roles
        this.keycloakService.addUserRolesRealm(userId, roleRepresentations);

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(new GenericApiResponse<>(
                        HttpStatus.NO_CONTENT,
                        HttpStatus.NO_CONTENT.value(),
                        "The roles have been successfully assigned",
                        BASE_ADMIN + GET_REALM_ROLES,
                        null
                ));

    }


    @Tag(name = "Assignation de rôles client",
            description = "Opérations d'ajout de rôles à un utilisateur dans une sous domain client")
    @Operation(
            summary = "Ajoute des rôles client à un utilisateur",
            description = "Assigne une ou plusieurs rôles client à un utilisateur spécifique dans Keycloak."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Les rôles ont été correctement assignés.",
                    content = @Content(schema = @Schema(implementation = GenericApiResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "Requête invalide : ID utilisateur ou liste des rôles manquants ou invalides.",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "401",
                    description = "Non autorisé (token expiré ou manquant)",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "403",
                    description = "Accès interdit",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "404",
                    description = "Utilisateur ou client introuvable",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "500",
                    description = "Erreur technique inattendue",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
    })
    @PostMapping(value = ADD_USER_ROLE_CLIENT)
    public ResponseEntity<GenericApiResponse<Void>> addUserRolesClient(
            @PathVariable String userId,
            @PathVariable String targetClient,
            @RequestBody List<RoleRepresentation> roleRepresentations
    ) {

        Optional<ResponseEntity<GenericApiResponse<Void>>> validationResponse = this.validateUserIdAndRoles(
                userId,
                roleRepresentations);

        if (validationResponse.isPresent()) {
            return validationResponse.get();
        }

        this.keycloakService.addUserRolesClient(userId, targetClient, roleRepresentations);

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(new GenericApiResponse<>(
                        HttpStatus.NO_CONTENT,
                        HttpStatus.NO_CONTENT.value(),
                        "The client roles have been successfully assigned to the user.",
                        BASE_ADMIN + ADD_USER_ROLE_CLIENT,
                        null
                ));
    }

    @Tag(name = "Supprimer un role Realm")
    @Operation(
            summary = "Supprime des rôles Realm à un utilisateur",
            description = "Supprime un ou plusieurs rôles Realm d’un utilisateur spécifique dans Keycloak."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Les rôles ont été correctement supprimés.",
                    content = @Content(schema = @Schema(implementation = GenericApiResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requête invalide : ID utilisateur ou liste des rôles manquants ou invalides.",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Non autorisé (token expiré ou manquant)",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Accès interdit",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Utilisateur ou rôle introuvable",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Erreur technique inattendue",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
    })
    @DeleteMapping(value = REMOVE_REALM_ROLE_FROM_USER)
    public ResponseEntity<GenericApiResponse<Void>> deleteRoleRealm(
            @PathVariable String userId,
            @RequestBody List<RoleRepresentation> roleRepresentations
    ) {

        Optional<ResponseEntity<GenericApiResponse<Void>>> validationResponse = this.validateUserIdAndRoles(
                userId,
                roleRepresentations);

        if (validationResponse.isPresent()) {
            return validationResponse.get();
        }

        this.keycloakService.removeRealmRoleFromUser(userId, roleRepresentations);

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(new GenericApiResponse<Void>(
                        HttpStatus.NO_CONTENT,
                        HttpStatus.NO_CONTENT.value(),
                        "The user was Successfully create",
                        BASE_ADMIN + REMOVE_REALM_ROLE_FROM_USER,
                        null
                ));
    }


    @Tag(name = "Supprimer un role client")
    @Operation(
            summary = "Supprime des rôles client d’un utilisateur",
            description = "Retire un ou plusieurs rôles client attribués à un utilisateur spécifique dans Keycloak."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204",
                    description = "Les rôles ont été correctement supprimés.",
                    content = @Content(schema = @Schema(implementation = GenericApiResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "Requête invalide : ID utilisateur ou liste des rôles manquants ou invalides.",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "401",
                    description = "Non autorisé (token expiré ou manquant)",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "403",
                    description = "Accès interdit",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "404",
                    description = "Utilisateur ou client introuvable",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "500",
                    description = "Erreur technique inattendue",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
    })
    @DeleteMapping(value = REMOVE_USER_ROLE_CLIENT)
    public ResponseEntity<GenericApiResponse<Void>> deleteRoleClient(
            @PathVariable String userId,
            @PathVariable String targetClient,
            @RequestBody List<RoleRepresentation> roleRepresentations
    ) {

        Optional<ResponseEntity<GenericApiResponse<Void>>> validationResponse = this.validateUserIdAndRoles(
                userId,
                roleRepresentations);

        if (validationResponse.isPresent()) {
            return validationResponse.get();
        }

        this.keycloakService.removeClientRoleFromUser(userId,targetClient, roleRepresentations);

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(new GenericApiResponse<Void>(
                        HttpStatus.NO_CONTENT,
                        HttpStatus.NO_CONTENT.value(),
                        "The user was Successfully create",
                        BASE_ADMIN + REMOVE_USER_ROLE_CLIENT,
                        null
                ));
    }

    /**
     * Valide les paramètres nécessaires à l'ajout ou à la suppression de rôles pour un utilisateur Keycloak.
     * <p>
     * Cette méthode vérifie que :
     * <ul>
     *     <li>La liste des rôles n'est pas {@code null} ni vide.</li>
     *     <li>L'identifiant de l'utilisateur est non vide.</li>
     * </ul>
     * Si l'une des conditions n'est pas remplie, une réponse HTTP 400 (Bad Request) est retournée, encapsulée dans un
     * {@link ResponseEntity} avec un message d'erreur explicite et un corps de réponse structuré
     * via {@link GenericApiResponse}.
     *
     * @param userId              l'identifiant Keycloak de l'utilisateur cible.
     * @param roleRepresentations la liste des rôles à valider.
     * @return un {@link Optional} contenant une {@link ResponseEntity} en cas d'erreur de validation,
     * ou un {@link Optional#empty()} si tout est valide.
     */
    protected Optional<ResponseEntity<GenericApiResponse<Void>>> validateUserIdAndRoles(
            String userId, List<RoleRepresentation> roleRepresentations
    ) {
        if (roleRepresentations == null || roleRepresentations.isEmpty()) {
            return Optional.of(ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new GenericApiResponse<>(
                            HttpStatus.BAD_REQUEST,
                            HttpStatus.BAD_REQUEST.value(),
                            "The list of roles cannot be empty.",
                            BASE_ADMIN + ADD_REALM_ROLE_TO_USER,
                            null
                    )));
        }

        if (!StringUtils.hasText(userId)) {
            return Optional.of(ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new GenericApiResponse<>(
                            HttpStatus.BAD_REQUEST,
                            HttpStatus.BAD_REQUEST.value(),
                            "The user ID is missing",
                            BASE_ADMIN + ADD_REALM_ROLE_TO_USER,
                            null
                    )));
        }

        return Optional.empty();
    }

}
