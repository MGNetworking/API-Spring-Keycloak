package com.nutrition.API_nutrition.controller;

import com.nutrition.API_nutrition.model.dto.ApiResponseData;
import com.nutrition.API_nutrition.model.dto.RegisterRequestDto;
import com.nutrition.API_nutrition.model.dto.TokenResponseDto;
import com.nutrition.API_nutrition.model.dto.UserResponseDto;
import com.nutrition.API_nutrition.model.entity.User;
import com.nutrition.API_nutrition.model.response.GenericApiResponse;
import com.nutrition.API_nutrition.service.KeycloakService;
import com.nutrition.API_nutrition.service.UserService;
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
    @PostMapping
    public ResponseEntity<GenericApiResponse<ApiResponseData>> postUser(
            @Valid @RequestBody RegisterRequestDto userDto) {

        if (this.keycloakService.userExistsById(userDto.getKeycloakId())) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new GenericApiResponse<ApiResponseData>(
                            HttpStatus.CONFLICT,
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
                        "The user was Successfully create",
                        "/api/v1/auth",
                        userResponseDto
                ));
    }

    // Mise à jour d'un utilisateur
    @PutMapping(value = "/user")
    public ResponseEntity<GenericApiResponse<ApiResponseData>> updateUser(
            @Valid @RequestBody RegisterRequestDto userDto) {

        // check si l'user exist dans keycloak
        if (!this.keycloakService.userExistsById(userDto.getKeycloakId())) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new GenericApiResponse<ApiResponseData>(
                            HttpStatus.NOT_FOUND,
                            "The update was a failure",
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
                            "This user is update with successfully",
                            "/api/v1/auth/user",
                            dto
                    ));
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new GenericApiResponse<ApiResponseData>(
                        HttpStatus.BAD_REQUEST,
                        "The update was a failure",
                        "/api/v1/auth/user",
                        userDto
                ));

    }

    @DeleteMapping(value = "/{userId}")
    public ResponseEntity<GenericApiResponse<String>> deleteUser(
            @PathVariable String userId) {

        if (!this.keycloakService.userExistsById(userId)) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new GenericApiResponse<String>(
                            HttpStatus.NOT_FOUND,
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
                        "This user is delete with successfully",
                        "/api/v1/auth/" + userId,
                        null
                ));


    }

    @GetMapping(value = "/{userId}")
    public ResponseEntity<GenericApiResponse<ApiResponseData>> getUser(
            @PathVariable String userId) {

        // Vérifier d'abord si l'utilisateur existe dans Keycloak
        if (!this.keycloakService.userExistsById(userId)) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new GenericApiResponse<ApiResponseData>(
                            HttpStatus.NOT_FOUND,
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
                            "Utilisateur trouver avec succès",
                            "/api/v1/auth/" + userId,
                            dtoUser
                    ));
        } else {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new GenericApiResponse<ApiResponseData>(
                            HttpStatus.NOT_FOUND,
                            "The user is not found in DB , but exist in keycloak",
                            "/api/v1/auth/" + userId,
                            null
                    ));
        }


    }
}
