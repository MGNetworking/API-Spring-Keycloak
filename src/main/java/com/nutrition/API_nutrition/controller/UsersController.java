package com.nutrition.API_nutrition.controller;

import com.nutrition.API_nutrition.model.dto.ApiResponseData;
import com.nutrition.API_nutrition.model.dto.UserCreatedResponseDto;
import com.nutrition.API_nutrition.model.dto.UserInputDTO;
import com.nutrition.API_nutrition.model.entity.User;
import com.nutrition.API_nutrition.model.response.GenericApiErrorResponse;
import com.nutrition.API_nutrition.model.response.GenericApiResponse;
import com.nutrition.API_nutrition.model.validation.OnCreate;
import com.nutrition.API_nutrition.security.AccessKeycloak;
import com.nutrition.API_nutrition.service.KeycloakService;
import com.nutrition.API_nutrition.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(UsersController.BASE_USERS)
@Tag(
        name = "Authentification",
        description = "Endpoints liés à la connexion, déconnexion et au rafraîchissement du token JWT."
)
@RequiredArgsConstructor
public class UsersController {

    public static final String BASE_USERS = "/api/v1/users";
    public static final String REGISTER = "/register";
    public static final String UPDATE_USER = "/user";
    public static final String DELETE_USER = "/{userId}";
    public static final String GET_USER_ID = "/{userId}";

    private final UserService userService;
    private final AccessKeycloak accessKeycloak;

    /**
     * Crée un nouvel utilisateur et retourne son profil avec un token JWT.
     *
     * <p>Si l'utilisateur existe déjà, renvoie HTTP 409 (Conflict).
     * Sinon, crée l'utilisateur et renvoie HTTP 201 (Created) avec le token.
     *
     * @return {@link ResponseEntity} avec {@link GenericApiResponse} :
     * HTTP 201, 400, 401, 403, 409 ou 500 selon le cas.
     */
    @Tag(name = "New user create")
    @Operation(
            summary = "Créer un nouvel utilisateur",
            description = "Crée un utilisateur dans le système et retourne son profil avec un token d'authentification."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Création réussie: l'utilisateur a était créé avec succès",
                    content = @Content(schema = @Schema(implementation = GenericApiResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "Requête invalide: paramètres manquants ou malformés.",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "401",
                    description = "Informations d'identification invalides: l'utilisateur n'est pas authentifié. ",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "403",
                    description = "Accès interdit: l'utilisateur n'a pas les droits nécessaires.",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "409",
                    description = "Conflict: L'utilisateur existe déjà",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "500",
                    description = "Erreur interne du serveur: une exception technique est survenue.",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class)))
    })
    @PostMapping(value = REGISTER)
    @Validated(OnCreate.class)
    public ResponseEntity<GenericApiResponse<ApiResponseData>> postUser() {

        String userId = this.accessKeycloak.getUserIdFromToken();
        this.userService.createUser(userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new GenericApiResponse<ApiResponseData>(
                        HttpStatus.CREATED,
                        HttpStatus.CREATED.value(),
                        "The user was Successfully create",
                        BASE_USERS + REGISTER,
                        new UserCreatedResponseDto(userId, "new user create")
                ));
    }

    /**
     * Met à jour un utilisateur existant.
     *
     * <p>Si l'utilisateur est introuvable, renvoie HTTP 404.
     * Sinon, met à jour et renvoie HTTP 200 (OK).
     *
     * @param userInputDTO Données à mettre à jour, validées.
     * @return {@link ResponseEntity} avec {@link GenericApiResponse} :
     * HTTP 200, 400, 401, 403, 404 ou 500 selon le cas.
     */
    @Tag(name = "Update user")
    @Operation(
            summary = " Met à jour les informations d'un utilisateur",
            description = "Met à jour les informations d'un utilisateur existant dans le système d'information."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Mise à jours : l'utilisateur à était mise à jour avec succès",
                    content = @Content(schema = @Schema(implementation = GenericApiResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "Requête invalide : paramètres manquants ou malformés.",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "401",
                    description = "Informations d'identification invalides: l'utilisateur n'est pas authentifié. ",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "403",
                    description = "Accès interdit : l'utilisateur n'a pas les droits nécessaires.",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "404",
                    description = "Non trouver : l'utilisateur n'a pas était trouver.",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "500",
                    description = "Erreur interne du serveur : une exception technique est survenue.",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class)))
    })
    @PutMapping(value = UPDATE_USER)
    @PreAuthorize("@accessKeycloak.hasAccessToUser(#userInputDTO.keycloakId)")
    public ResponseEntity<GenericApiResponse<ApiResponseData>> updateUser(
            @Valid @RequestBody UserInputDTO userInputDTO) {

        User updateUser = this.userService.updateUser(userInputDTO);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new GenericApiResponse<ApiResponseData>(
                        HttpStatus.OK,
                        HttpStatus.OK.value(),
                        "This user is update with successfully",
                        BASE_USERS + UPDATE_USER,
                        new UserCreatedResponseDto(updateUser.getKeycloakId(), "User created")
                ));

    }

    /**
     * Supprime un utilisateur via son identifiant Keycloak.
     *
     * <p>Si l'utilisateur est introuvable, renvoie HTTP 404.
     * Sinon, supprime et renvoie HTTP 204 (No Content).
     *
     * @param userId Identifiant Keycloak de l'utilisateur.
     * @return {@link ResponseEntity} avec {@link GenericApiResponse} :
     * HTTP 204, 400, 401, 403, 404, 409 ou 500 selon le cas.
     */
    @Tag(name = "Delete user")
    @Operation(
            summary = "Supprime un utilisateur",
            description = "Supprime un utilisateur existant à partir de son identifiant Keycloak"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204",
                    description = "Suppression réussi : l'utilisateur à était supprimer avec succès",
                    content = @Content(schema = @Schema(implementation = GenericApiResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "Requête invalide",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "401",
                    description = "Informations d'identification invalides: l'utilisateur n'est pas authentifié. ",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "403",
                    description = "Accès interdit : l'utilisateur n'a pas les droits nécessaires.",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "404",
                    description = "Non trouver : l'utilisateur n'a pas était trouver.",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "409",
                    description = "Conflict : L'utilisateur n'a pas était supprimer dans le service Keycloak",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "500",
                    description = "Erreur interne du serveur : une exception technique est survenue.",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class)))
    })
    @DeleteMapping(value = DELETE_USER)
    @PreAuthorize("@accessKeycloak.hasAccessToUser(#userId)")
    public ResponseEntity<GenericApiResponse<String>> deleteUser(@PathVariable String userId) {

        this.userService.deleteUser(userId);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(new GenericApiResponse<String>(
                        HttpStatus.NO_CONTENT,
                        HttpStatus.NO_CONTENT.value(),
                        "This user is delete with successfully",
                        BASE_USERS + "/" + userId,
                        null
                ));
    }

    /**
     * Récupère les informations d’un utilisateur à partir de son identifiant Keycloak.
     *
     * @param userId Identifiant unique Keycloak de l'utilisateur.
     * @return Réponse HTTP contenant les données utilisateur.
     */
    @Tag(name = "Get user")
    @Operation(
            summary = "Récupère les informations d’un utilisateur",
            description = "Récupère les informations d’un utilisateur à partir de son identifiant Keycloak"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "L'utilisateur à était trouver avec succès",
                    content = @Content(schema = @Schema(implementation = GenericApiResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "Requête invalide",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "401",
                    description = "Informations d'identification invalides: l'utilisateur n'est pas authentifié. ",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "403",
                    description = "Accès interdit : l'utilisateur n'a pas les droits nécessaires.",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "404",
                    description = "Non trouver : l'utilisateur n'a pas était trouver.",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "500",
                    description = "Erreur interne du serveur : une exception technique est survenue.",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class)))
    })
    @GetMapping(value = GET_USER_ID)
    @PreAuthorize("@accessKeycloak.hasAccessToUser(#userId)")
    public ResponseEntity<GenericApiResponse<ApiResponseData>> getUser(@PathVariable String userId) {

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new GenericApiResponse<ApiResponseData>(
                        HttpStatus.OK,
                        HttpStatus.OK.value(),
                        "user find successfully",
                        BASE_USERS + "/" + userId,
                        userService.getUser(userId)
                ));
    }
}
