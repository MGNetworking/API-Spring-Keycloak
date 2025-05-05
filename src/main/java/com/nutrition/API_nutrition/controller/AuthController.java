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
     * Permet l'enregistrement d'un utilisateur
     *
     * @param userDto RegisterRequestDto
     * @return GenericApiResponse<ApiResponseData>
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

        if (this.keycloakService.userExistsById(userDto.getKeycloakId())) {
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

    // Mise à jour d'un utilisateur
    @Tag(name = "update user")
    @PutMapping(value = "/user")
    public ResponseEntity<GenericApiResponse<ApiResponseData>> updateUser(
            @Valid @RequestBody RegisterRequestDto userDto) {

        // check si l'user exist dans keycloak
        if (!this.keycloakService.userExistsById(userDto.getKeycloakId())) {
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

    @Tag(name = "delete user")
    @DeleteMapping(value = "/{userId}")
    public ResponseEntity<GenericApiResponse<String>> deleteUser(
            @PathVariable String userId) {

        if (!this.keycloakService.userExistsById(userId)) {
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

    @Tag(name = "Get user")
    @GetMapping(value = "/{userId}")
    public ResponseEntity<GenericApiResponse<ApiResponseData>> getUser(
            @PathVariable String userId) {

        // Vérifier d'abord si l'utilisateur existe dans Keycloak
        if (!this.keycloakService.userExistsById(userId)) {
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
