package com.nutrition.API_nutrition.controller;

import com.nutrition.API_nutrition.model.dto.ApiResponseData;
import com.nutrition.API_nutrition.model.dto.RegisterRequestDto;
import com.nutrition.API_nutrition.model.dto.TokenResponseDto;
import com.nutrition.API_nutrition.model.dto.UserResponseDto;
import com.nutrition.API_nutrition.model.entity.User;
import com.nutrition.API_nutrition.model.response.GenericApiResponse;
import com.nutrition.API_nutrition.service.KeycloakService;
import com.nutrition.API_nutrition.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Keycloak User Management", description = "Operations related to users")
@RequiredArgsConstructor
public class AuthController {

    private final KeycloakService keycloakService;
    private final UserService userService;

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
     *     <li>HTTP 409 si l'utilisateur existe déjà.</li>
     *     <li>HTTP 400 en cas de requête invalide (gérée par la validation Spring).</li>
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
    @Tag(name = "login")
    @Operation(
            summary = "Créer un nouvel utilisateur",
            description = "Crée un utilisateur dans le système et retourne son profil avec un token d'authentification."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Utilisateur créé avec succès"),
            @ApiResponse(responseCode = "409", description = "L'utilisateur existe déjà"),
            @ApiResponse(responseCode = "400", description = "Requête invalide", content = @Content),
    })
    @PostMapping
    public ResponseEntity<GenericApiResponse<ApiResponseData>> postUser(
            @Valid @RequestBody RegisterRequestDto userDto) {

        if (this.keycloakService.checkUserExist(userDto)) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new GenericApiResponse<ApiResponseData>(
                            HttpStatus.CONFLICT,
                            HttpStatus.CONFLICT.value(),
                            "The user is already exists",
                            "/api/v1/auth",
                            userDto
                    ));
        }

        // Créer un user
        UserResponseDto userResponseDto = this.userService.createUser(userDto);
        TokenResponseDto tokens = keycloakService.login(
                userDto.getUserName(),
                userDto.getPassword());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header("Authorization", "Bearer " + tokens.getAccessToken())
                .body(new GenericApiResponse<ApiResponseData>(
                        HttpStatus.CREATED,
                        HttpStatus.CREATED.value(),
                        "The user was Successfully create",
                        "/api/v1/auth",
                        userResponseDto
                ));
    }

    /**
     * Met à jour les informations d'un utilisateur existant dans le système.
     *
     * <p>Cette méthode effectue les étapes suivantes :
     * <ol>
     *     <li>Vérifie si l'utilisateur existe dans Keycloak via {@code keycloakService.checkUserExist()}.</li>
     *     <li>Si l'utilisateur n'existe pas, renvoie une réponse HTTP 404 (Not Found).</li>
     *     <li>Sinon, tente de mettre à jour l'utilisateur via {@code userService.updateUser()}.</li>
     *     <li>Si la mise à jour réussit (présence d'un {@code keycloakId}), renvoie une réponse HTTP 200 (OK) avec les données mises à jour.</li>
     *     <li>Si la mise à jour échoue, renvoie une réponse HTTP 400 (Bad Request).</li>
     * </ol>
     *
     * @param userDto Les données de l'utilisateur à mettre à jour. Validées automatiquement grâce à {@code @Valid}.
     * @return Une {@link ResponseEntity} contenant un {@link GenericApiResponse} avec :
     * <ul>
     *     <li>HTTP 200 si la mise à jour réussit.</li>
     *     <li>HTTP 400 si la mise à jour échoue.</li>
     *     <li>HTTP 404 si l'utilisateur n'existe pas.</li>
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
    @PutMapping(value = "/user")
    public ResponseEntity<GenericApiResponse<ApiResponseData>> updateUser(
            @Valid @RequestBody RegisterRequestDto userDto) {

        // check si l'user exist dans keycloak
        if (this.keycloakService.checkUserExist(userDto)) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new GenericApiResponse<ApiResponseData>(
                            HttpStatus.NOT_FOUND,
                            HttpStatus.NOT_FOUND.value(),
                            "User not exist, Update is impossible",
                            "/api/v1/auth/user",
                            null
                    ));
        }

        UserResponseDto dto = this.userService.updateUser(userDto);
        if (dto.getKeycloakId() != null) {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(new GenericApiResponse<ApiResponseData>(
                            HttpStatus.OK,
                            HttpStatus.OK.value(),
                            "This user is update with successfully",
                            "/api/v1/auth/user",
                            dto
                    ));
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new GenericApiResponse<ApiResponseData>(
                        HttpStatus.BAD_REQUEST,
                        HttpStatus.BAD_REQUEST.value(),
                        "The update was a failure",
                        "/api/v1/auth/user",
                        userDto
                ));

    }

    /**
     * Supprime un utilisateur existant à partir de son identifiant Keycloak.
     *
     * <p>Cette méthode :
     * <ol>
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
     *     <li>HTTP 404 si l'utilisateur n'existe pas.</li>
     * </ul>
     * @see RegisterRequestDto
     * @see GenericApiResponse
     * @see KeycloakService#checkUserExist(RegisterRequestDto)
     * @see UserService#deleteUser(String)
     */
    @Tag(name = "delete user")
    @DeleteMapping(value = "/{userId}")
    public ResponseEntity<GenericApiResponse<String>> deleteUser(
            @PathVariable String userId) {

        RegisterRequestDto dto = new RegisterRequestDto();
        dto.setKeycloakId(userId);

        if (this.keycloakService.checkUserExist(dto)) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new GenericApiResponse<String>(
                            HttpStatus.NOT_FOUND,
                            HttpStatus.NOT_FOUND.value(),
                            "The user is not found",
                            "/api/v1/auth/" + userId,
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
                        "/api/v1/auth/" + userId,
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
     *     <li>HTTP 200 si l'utilisateur est trouvé en base de données.</li>
     *     <li>HTTP 404 si l'utilisateur n'existe pas (dans Keycloak ou la base).</li>
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
    @GetMapping(value = "/{userId}")
    public ResponseEntity<GenericApiResponse<ApiResponseData>> getUser(
            @PathVariable String userId) {

        RegisterRequestDto dto = new RegisterRequestDto();
        dto.setKeycloakId(userId);

        // Vérifier d'abord si l'utilisateur existe dans Keycloak
        if (this.keycloakService.checkUserExist(dto)) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new GenericApiResponse<ApiResponseData>(
                            HttpStatus.NOT_FOUND,
                            HttpStatus.NOT_FOUND.value(),
                            "The user is not found",
                            "/api/v1/auth/" + userId,
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
                            "/api/v1/auth/" + userId,
                            dtoUser
                    ));
        } else {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new GenericApiResponse<ApiResponseData>(
                            HttpStatus.NOT_FOUND,
                            HttpStatus.NOT_FOUND.value(),
                            "The user is not found in DB , but exist in keycloak",
                            "/api/v1/auth/" + userId,
                            null
                    ));
        }
    }
}
