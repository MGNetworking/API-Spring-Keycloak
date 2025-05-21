package com.nutrition.API_nutrition.controller;

import com.nutrition.API_nutrition.model.dto.*;
import com.nutrition.API_nutrition.model.entity.User;
import com.nutrition.API_nutrition.model.response.GenericApiErrorResponse;
import com.nutrition.API_nutrition.model.response.GenericApiResponse;
import com.nutrition.API_nutrition.model.validation.OnCreateOrUpdateUser;
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

import java.util.Optional;

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

    private final KeycloakService keycloakService;
    private final UserService userService;
    private final AccessKeycloak accessKeycloak;

    /**
     * Crée un nouvel utilisateur dans le système et retourne son profil accompagné d’un token JWT.
     *
     * <p>Cette méthode suit le processus suivant :
     * <ol>
     *     <li>Vérifie si l'utilisateur existe déjà dans Keycloak via {@code keycloakService.checkUserExist()}.</li>
     *     <li>Si l'utilisateur existe, renvoie une réponse HTTP 409 (Conflict).</li>
     *     <li>Sinon, crée l'utilisateur via {@code userService.createUser()}.</li>
     *     <li>Connecte automatiquement l'utilisateur via {@code keycloakService.login()} pour générer un token JWT.</li>
     *     <li>Renvoie une réponse HTTP 201 (Created) avec les informations de l'utilisateur et un header {@code Authorization} contenant le token.</li>
     * </ol>
     *
     * @param userDto Les informations du nouvel utilisateur à créer. L'objet est validé grâce à {@code @Valid}.
     * @return Une {@link ResponseEntity} contenant un {@link GenericApiResponse} avec :
     * <ul>
     *     <li>HTTP 201 si la création est réussie (avec token JWT).</li>
     *     <li>HTTP 400 en cas de requête invalide (gérée par la validation Spring).</li>
     *     <li>HTTP 401 si les informations d'identification invalides</li>
     *     <li>HTTP 403 si l'accès interdit.</li>
     *     <li>HTTP 409 si l'utilisateur existe déjà.</li>
     *     <li>HTTP 500 si une erreur interne du serveur </li>
     * </ul>
     * @see RegisterRequestDto
     * @see UserResponseDto
     * @see TokenResponseDto
     * @see GenericApiResponse
     * @see ApiResponseData
     * @see KeycloakService#checkUserExist(RegisterRequestDto)
     * @see KeycloakService#login(String, String)
     * @see UserService#createUser(RegisterRequestDto)
     */
    @Tag(name = "register")
    @Operation(
            summary = "Créer un nouvel utilisateur",
            description = "Crée un utilisateur dans le système et retourne son profil avec un token d'authentification."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Création réussie : l'utilisateur a était créé avec succès",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "Requête invalide : paramètres manquants ou malformés.",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "401",
                    description = "Informations d'identification invalides: l'utilisateur n'est pas authentifié. ",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "403",
                    description = "Accès interdit : l'utilisateur n'a pas les droits nécessaires.",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "409",
                    description = "Conflict : L'utilisateur existe déjà",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "500",
                    description = "Erreur interne du serveur : une exception technique est survenue.",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class)))
    })
    @PostMapping(value = REGISTER)
    @Validated(OnCreateOrUpdateUser.class)
    public ResponseEntity<GenericApiResponse<ApiResponseData>> postUser(@Valid @RequestBody RegisterRequestDto userDto) {

        if (this.keycloakService.checkUserExist(userDto)) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new GenericApiResponse<ApiResponseData>(
                            HttpStatus.CONFLICT,
                            HttpStatus.CONFLICT.value(),
                            "The user is already exists",
                            BASE_USERS + REGISTER,
                            userDto
                    ));
        }

        // Créer un user
        UserResponseDto userResponseDto = this.userService.createUser(userDto);
        TokenResponseDto token = keycloakService.login(
                userDto.getKeycloakUserData().getUserName(),
                userDto.getKeycloakUserData().getPassword());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new GenericApiResponse<ApiResponseData>(
                        HttpStatus.CREATED,
                        HttpStatus.CREATED.value(),
                        "The user was Successfully create",
                        BASE_USERS + REGISTER,
                        new ResponsUserTokenDto(
                                userResponseDto,
                                token)
                ));
    }


    /**
     * Met à jour les informations d'un utilisateur existant dans le système d'information.
     *
     * <p>Cette méthode effectue les étapes suivantes :
     * <ol>
     *     <li>Vérifie si l'utilisateur existe dans Keycloak via {@code keycloakService.checkUserExist()}.</li>
     *     <li>Si l'utilisateur n'existe pas, renvoie une réponse HTTP 404 (Not Found).</li>
     *     <li>Sinon, tente de mettre à jour l'utilisateur via {@code userService.updateUser()}.</li>
     *     <li>Si la mise à jour réussit (présence d'un {@code keycloakId}), renvoie une réponse HTTP 200 (OK) avec les
     *     données mises à jour.</li>
     *     <li>Si la mise à jour échoue, renvoie une réponse HTTP 400 (Bad Request).</li>
     * </ol>
     *
     * @param userDto    Données de l'utilisateur à mettre à jour (inclut l'identifiant Keycloak).
     *                   Validées avec {@code @Valid}.
     * @param authHeader En-tête HTTP Authorization contenant le token Bearer pour valider l'accès.
     * @return Une {@link ResponseEntity} contenant un {@link GenericApiResponse} avec :
     * <ul>
     *     <li>HTTP 200 si la mise à jour réussit.</li>
     *     <li>HTTP 400 en cas de requête invalide (gérée par la validation Spring).</li>
     *     <li>HTTP 401 si les informations d'identification invalides</li>
     *     <li>HTTP 403 si l'accès interdit.</li>
     *     <li>HTTP 404 si l'utilisateur n'a pas était trouvé</li>
     *     <li>HTTP 409 si l'utilisateur existe déjà.</li>
     *     <li>HTTP 500 si une erreur interne du serveur </li>
     *
     * </ul>
     * @see RegisterRequestDto
     * @see UserResponseDto
     * @see GenericApiResponse
     * @see ApiResponseData
     * @see KeycloakService#checkUserExist(RegisterRequestDto)
     * @see UserService#updateUser(RegisterRequestDto)
     */
    @Tag(name = "update user")
    @Operation(
            summary = " Met à jour les informations d'un utilisateur",
            description = "Met à jour les informations d'un utilisateur existant dans le système d'information."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Mise à jours : l'utilisateur à était mise à jour avec succès",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
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
    @PreAuthorize("@accessKeycloak.isAuthenticatedAndAuthorized(#userDto.keycloakId)")
    public ResponseEntity<GenericApiResponse<ApiResponseData>> updateUser(
            @Valid @RequestBody RegisterRequestDto userDto,
            @RequestHeader("Authorization") String authHeader) {

        String token = this.accessKeycloak.extractToken(authHeader);

        // check si l'user exist dans keycloak
        if (this.keycloakService.checkUserExist(userDto)) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new GenericApiResponse<ApiResponseData>(
                            HttpStatus.NOT_FOUND,
                            HttpStatus.NOT_FOUND.value(),
                            "User not exist, Update is impossible",
                            BASE_USERS + UPDATE_USER,
                            null
                    ));
        }

        // Met à jour l'objet
        UserResponseDto userResponseDto = this.userService.updateUser(userDto);

        // met à jour le token
        TokenResponseDto tokenDto = this.keycloakService.refreshToken(token);

        if (userResponseDto.getKeycloakId() != null) {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(new GenericApiResponse<ApiResponseData>(
                            HttpStatus.OK,
                            HttpStatus.OK.value(),
                            "This user is update with successfully",
                            BASE_USERS + UPDATE_USER,
                            new ResponsUserTokenDto(userResponseDto, tokenDto)
                    ));
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new GenericApiResponse<ApiResponseData>(
                        HttpStatus.BAD_REQUEST,
                        HttpStatus.BAD_REQUEST.value(),
                        "The update was a failure",
                        BASE_USERS + UPDATE_USER,
                        userDto
                ));

    }


    /**
     * Supprime un utilisateur existant à partir de son identifiant Keycloak.
     *
     * <p>Cette méthode :
     * <ol>
     *     <li>Vérifie les droits d'accès de l'utilisateur appelant via {@link AccessKeycloak#hasAccessToUser(String)}.</li>
     *     <li>Construit un objet {@link RegisterRequestDto} avec l'identifiant utilisateur.</li>
     *     <li>Vérifie si l'utilisateur existe dans Keycloak via {@code keycloakService.checkUserExist()}.</li>
     *     <li>Si l'utilisateur n'existe pas, renvoie une réponse HTTP 404 (Not Found).</li>
     *     <li>Sinon, supprime l'utilisateur via {@code userService.deleteUser()}.</li>
     *     <li>Renvoie une réponse HTTP 204 (No Content) indiquant que la suppression a été effectuée avec succès.</li>
     * </ol>
     *
     * @param userId L'identifiant unique de l'utilisateur à supprimer (Keycloak ID).
     * @return Une {@link ResponseEntity} contenant un {@link GenericApiResponse} avec :
     * <ul>
     *     <li>HTTP 204 si la suppression est réussie.</li>
     *     <li>HTTP 400 si la suppression est invalide.</li>
     *     <li>HTTP 401 si les informations d'identification invalides</li>
     *     <li>HTTP 403 si l'accès interdit.</li>
     *     <li>HTTP 404 si l'utilisateur n'a pas était trouvé</li>
     *     <li>HTTP 409 si conflict intervient pendant la suppréssion</li>
     *     <li>HTTP 500 si une erreur interne du serveur </li>
     * </ul>
     * @see RegisterRequestDto
     * @see GenericApiResponse
     * @see KeycloakService#checkUserExist(RegisterRequestDto)
     * @see UserService#deleteUser(String)
     */
    @Tag(name = "delete user")
    @Operation(
            summary = "Supprime un utilisateur",
            description = "Supprime un utilisateur existant à partir de son identifiant Keycloak"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204",
                    description = "Suppression réussi : l'utilisateur à était supprimer avec succès",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
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
    @PreAuthorize("@accessKeycloak.isAuthenticatedAndAuthorized(#userId)")
    public ResponseEntity<GenericApiResponse<String>> deleteUser(@PathVariable String userId) {

        RegisterRequestDto dto = new RegisterRequestDto();
        dto.getKeycloakUserData().setKeycloakId(userId);

        if (this.keycloakService.checkUserExist(dto)) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new GenericApiResponse<String>(
                            HttpStatus.NOT_FOUND,
                            HttpStatus.NOT_FOUND.value(),
                            "The user is not found",
                            BASE_USERS + "/" + userId,
                            null
                    ));
        }
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
     * <p>Cette méthode suit les étapes suivantes :
     * <ol>
     *     <li>Construit un {@link RegisterRequestDto} avec l'identifiant fourni.</li>
     *     <li>Vérifie si l'utilisateur existe dans Keycloak via {@code keycloakService.checkUserExist()}.</li>
     *     <li>Si l'utilisateur n'existe pas dans Keycloak, renvoie une réponse HTTP 404 (Not Found).</li>
     *     <li>Sinon, tente de récupérer l'utilisateur en base de données via {@code userService.getuser()}.</li>
     *     <li>Si trouvé en base, retourne ses informations dans un {@link UserResponseDto} avec HTTP 200.</li>
     *     <li>Sinon, retourne HTTP 404 avec un message indiquant que l'utilisateur est uniquement présent dans Keycloak.</li>
     * </ol>
     *
     * @param userId L'identifiant unique de l'utilisateur (Keycloak ID).
     * @return Une {@link ResponseEntity} contenant un {@link GenericApiResponse} avec :
     * <ul>
     *     <li>HTTP 200 si L'utilisateur à était trouver avec succès</li>
     *     <li>HTTP 400 si la suppression est invalide.</li>
     *     <li>HTTP 401 si les informations d'identification invalides</li>
     *     <li>HTTP 403 si l'accès interdit.</li>
     *     <li>HTTP 404 si l'utilisateur n'existe pas (dans Keycloak ou la base).</li>
     *     <li>HTTP 500 si une erreur interne du serveur </li>
     * </ul>
     * @see RegisterRequestDto
     * @see User
     * @see UserResponseDto
     * @see GenericApiResponse
     * @see ApiResponseData
     * @see KeycloakService#checkUserExist(RegisterRequestDto)
     * @see UserService#getuser(String)
     */
    @Tag(name = "Get user")
    @Operation(
            summary = "Récupère les informations d’un utilisateur",
            description = "Récupère les informations d’un utilisateur à partir de son identifiant Keycloak"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "L'utilisateur à était trouver avec succès",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
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

        RegisterRequestDto dto = new RegisterRequestDto();
        dto.getKeycloakUserData().setKeycloakId(userId);

        // Vérifier d'abord si l'utilisateur existe dans Keycloak
        if (this.keycloakService.checkUserExist(dto)) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new GenericApiResponse<ApiResponseData>(
                            HttpStatus.NOT_FOUND,
                            HttpStatus.NOT_FOUND.value(),
                            "The user is not found",
                            BASE_USERS + "/" + userId,
                            null
                    ));
        }

        Optional<User> user = this.userService.getuser(userId);

        // Si il est présent en DB
        if (user.isPresent()) {

            UserResponseDto dtoUser = new UserResponseDto().mappingToUser(user.get());
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(new GenericApiResponse<ApiResponseData>(
                            HttpStatus.OK,
                            HttpStatus.OK.value(),
                            "User successfully found",
                            BASE_USERS + "/" + userId,
                            dtoUser
                    ));
        } else {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new GenericApiResponse<ApiResponseData>(
                            HttpStatus.NOT_FOUND,
                            HttpStatus.NOT_FOUND.value(),
                            "The user is not found in DB , but exist in keycloak",
                            BASE_USERS + "/" + userId,
                            null
                    ));
        }
    }
}
