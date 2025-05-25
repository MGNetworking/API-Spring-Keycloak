package com.nutrition.API_nutrition.controller;

import com.nutrition.API_nutrition.model.response.GenericApiErrorResponse;
import com.nutrition.API_nutrition.model.response.GenericApiResponse;
import com.nutrition.API_nutrition.service.KeycloakService;
import com.nutrition.API_nutrition.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.RoleRepresentation;
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
    public static final String GET_CLIENT_ROLES = "/roles/client";

    // Ajout de rôles à un utilisateur
    public static final String ADD_REALM_ROLE_TO_USER = "/users/{userId}/roles/realm";
    public static final String ADD_CLIENT_ROLE_TO_USER = "/users/{userId}/roles/client";

    // Suppression de rôles d’un utilisateur
    public static final String REMOVE_REALM_ROLE_FROM_USER = "/users/{userId}/roles/realm";
    public static final String REMOVE_CLIENT_ROLE_FROM_USER = "/users/{userId}/roles/client";


    private final KeycloakService keycloakService;
    private final UserService userService;

    @Tag(name = "Rôles",
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
    @PreAuthorize("@accessKeycloak.isAuthenticatedAndAuthorized(#userDto.keycloakId)")
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
    @PreAuthorize("@accessKeycloak.isAuthenticatedAndAuthorized(#userDto.keycloakId)")
    public ResponseEntity<GenericApiResponse<List<RoleRepresentation>>> getListRolesClient() {

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new GenericApiResponse<List<RoleRepresentation>>(
                        HttpStatus.OK,
                        HttpStatus.OK.value(),
                        "The user was Successfully create",
                        BASE_ADMIN + GET_CLIENT_ROLES,
                        this.keycloakService.getClientScopedRoles()
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
    @PreAuthorize("@accessKeycloak.isAuthenticatedAndAuthorized(#userDto.keycloakId)")
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
    @PostMapping(value = ADD_CLIENT_ROLE_TO_USER)
    @PreAuthorize("@accessKeycloak.isAuthenticatedAndAuthorized(#userDto.keycloakId)")
    public ResponseEntity<GenericApiResponse<Void>> addUserRolesClient(
            @PathVariable String userId,
            @RequestBody List<RoleRepresentation> roleRepresentations
    ) {

        Optional<ResponseEntity<GenericApiResponse<Void>>> validationResponse = this.validateUserIdAndRoles(
                userId,
                roleRepresentations);

        if (validationResponse.isPresent()) {
            return validationResponse.get();
        }

        this.keycloakService.addUserRolesClient(userId, roleRepresentations);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new GenericApiResponse<>(
                        HttpStatus.OK,
                        HttpStatus.OK.value(),
                        "The client roles have been successfully assigned to the user.",
                        BASE_ADMIN + ADD_CLIENT_ROLE_TO_USER,
                        null
                ));
    }

    @Tag(name = "Supprimer un role Realm")
    @Operation(
            summary = "Supprime des rôles Realm à un utilisateur",
            description = "Supprime un ou plusieurs rôles Realm d’un utilisateur spécifique dans Keycloak."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Les rôles ont été correctement supprimés.",
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
    @PreAuthorize("@accessKeycloak.isAuthenticatedAndAuthorized(#userDto.keycloakId)")
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
                .status(HttpStatus.OK)
                .body(new GenericApiResponse<Void>(
                        HttpStatus.OK,
                        HttpStatus.OK.value(),
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
            @ApiResponse(responseCode = "200",
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
    @DeleteMapping(value = REMOVE_CLIENT_ROLE_FROM_USER)
    @PreAuthorize("@accessKeycloak.isAuthenticatedAndAuthorized(#userDto.keycloakId)")
    public ResponseEntity<GenericApiResponse<Void>> deleteRoleClient(
            @PathVariable String userId,
            @RequestBody List<RoleRepresentation> roleRepresentations
    ) {

        Optional<ResponseEntity<GenericApiResponse<Void>>> validationResponse = this.validateUserIdAndRoles(
                userId,
                roleRepresentations);

        if (validationResponse.isPresent()) {
            return validationResponse.get();
        }

        this.keycloakService.removeClientRoleFromUser(userId, roleRepresentations);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new GenericApiResponse<Void>(
                        HttpStatus.OK,
                        HttpStatus.OK.value(),
                        "The user was Successfully create",
                        BASE_ADMIN + REMOVE_CLIENT_ROLE_FROM_USER,
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
    private Optional<ResponseEntity<GenericApiResponse<Void>>> validateUserIdAndRoles(
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
