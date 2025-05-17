package com.nutrition.API_nutrition.controller;

import com.nutrition.API_nutrition.model.dto.ApiResponseData;
import com.nutrition.API_nutrition.model.response.GenericApiResponse;
import com.nutrition.API_nutrition.service.KeycloakService;
import com.nutrition.API_nutrition.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public final static String GET_USERS = "/users";
    public final static String UPDATE_USER_ID = "/users/{userId}";


    private final KeycloakService keycloakService;
    private final UserService userService;

/*    @Tag(name = "register")
    @Operation(
            summary = "Créer un nouvel utilisateur",
            description = "Crée un utilisateur dans le système et retourne son profil avec un token d'authentification."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Utilisateur créé avec succès"),
            @ApiResponse(responseCode = "409", description = "L'utilisateur existe déjà"),
            @ApiResponse(responseCode = "400", description = "Requête invalide", content = @Content),
    })
    @PostMapping(value = GET_USERS)
    public ResponseEntity<GenericApiResponse<ApiResponseData>> getUsers() {



    }

    @Tag(name = "register")
    @Operation(
            summary = "",
            description = ""
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = ""),
            @ApiResponse(responseCode = "404", description = ""),
    })
    @PostMapping(value = UPDATE_USER_ID)
    public ResponseEntity<GenericApiResponse<ApiResponseData>> UpdateUsers() {

    }*/
}
