package com.nutrition.API_nutrition.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nutrition.API_nutrition.model.dto.RegisterRequestDto;
import com.nutrition.API_nutrition.model.dto.TokenResponseDto;
import com.nutrition.API_nutrition.model.dto.UserResponseDto;
import com.nutrition.API_nutrition.model.entity.User;
import com.nutrition.API_nutrition.service.KeycloakService;
import com.nutrition.API_nutrition.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final KeycloakService keycloakService;
    private final UserService userService;

    @PostMapping(value = "/register")
    public ResponseEntity<UserResponseDto> postUser(@Valid @RequestBody RegisterRequestDto userDto) throws JsonProcessingException {

        log.info("User DTO {}", userDto);

        // Créer l'utilisateur dans Keycloak
        String keycloakId  = this.keycloakService.createUser(
                userDto.getUserName(),
                userDto.getEmail(),
                userDto.getPassword(),
                userDto.getFirstName(),
                userDto.getLastName()
        );

        // Attribuer le rôle de base "USER" à tous les nouveaux utilisateurs
        keycloakService.addUserRoles(keycloakId, List.of("USER"));


        userDto.setKeycloakId(keycloakId);

        // Enregistrement en DB du user
        UserResponseDto userResponseDto = this.userService.createUser(userDto);
        log.info("User Response Dto {}", userResponseDto);

        // Obtenir un token pour l'utilisateur
        TokenResponseDto tokens = keycloakService.login(userDto.getUserName(), userDto.getPassword());
        log.info("User Response Dto {}", userResponseDto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header("Authorization", "Bearer " + tokens.getAccessToken())
                .body(userResponseDto);
    }
}
