package com.nutrition.API_nutrition.controller;

import com.nutrition.API_nutrition.exception.ApiException;
import com.nutrition.API_nutrition.model.dto.ApiResponseData;
import com.nutrition.API_nutrition.model.dto.KeycloakLoginRequestDto;
import com.nutrition.API_nutrition.model.dto.TokenResponseDto;
import com.nutrition.API_nutrition.model.response.GenericApiErrorResponse;
import com.nutrition.API_nutrition.model.response.GenericApiResponse;
import com.nutrition.API_nutrition.security.AccessKeycloak;
import com.nutrition.API_nutrition.service.KeycloakService;
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
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(AuthController.BASE_AUTH)
@Tag(
        name = "Gestion Utilisateurs",
        description = "Endpoints permettant à l'utilisateur de gérer ses propres informations (CRUD personnel)."
)
@RequiredArgsConstructor
public class AuthController {

    public static final String BASE_AUTH = "/api/v1/auth";
    public static final String LOGIN = "/login";
    public static final String LOGOUT = "/logout";
    public static final String REFRESH = "/refresh";

    private final KeycloakService keycloakService;
    private final AccessKeycloak accessKeycloak;

    @Tag(name = "login")
    @Operation(
            summary = "Authentification utilisateur et génération de token",
            description = "Permet à un utilisateur de s'authentifier à l'aide de ses identifiants et de récupérer un " +
                    "token JWT à utiliser pour les appels ultérieurs sécurisés."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentification réussie. Un token d'accès est retourné.",
                    content = @Content(schema = @Schema(implementation = GenericApiResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requête invalide : paramètres manquants ou malformés.",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Identifiants incorrects : l'utilisateur n'est pas authentifié.",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Accès refusé : l'utilisateur n'a pas les droits nécessaires.",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Accès refusé : L'utilisateur n'existe pas",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Erreur interne du serveur : une exception technique est survenue.",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class)))
    })
    @PostMapping(value = LOGIN)
    public ResponseEntity<GenericApiResponse<ApiResponseData>> login(
            @Valid @RequestBody KeycloakLoginRequestDto dto) throws ApiException {

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new GenericApiResponse<ApiResponseData>(
                        HttpStatus.OK,
                        HttpStatus.OK.value(),
                        "User authenticated successfully.",
                        BASE_AUTH + LOGIN,
                        this.keycloakService.login(
                                dto.getUserName(),
                                dto.getPassword())
                ));

    }


    @Tag(name = "logout")
    @Operation(
            summary = "Permet la déconnexion du utilisateur",
            description = "Déconnecte l'utilisateur à partir de son token"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Déconnexion réussie",
                    content = @Content(schema = @Schema(implementation = GenericApiResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requête mal formée (ex: token invalide)",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Non autorisé (token expiré ou manquant)",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Accès interdit",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Erreur technique inattendue",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class)))
    })
    @PostMapping(value = LOGOUT)
    public ResponseEntity<GenericApiResponse<String>> logout() {

        this.keycloakService.logout(this.accessKeycloak.getUserIdFromToken());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new GenericApiResponse<String>(
                        HttpStatus.OK,
                        HttpStatus.OK.value(),
                        "User not exist, Update is impossible",
                        BASE_AUTH + LOGIN,
                        "Logout Successfully")
                );

    }

    @Tag(name = "refresh")
    @Operation(
            summary = "Rafraîchissement token",
            description = "Permet le rafraichissement du token jwt et lui retourne un token accès"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Rafraîchissement du token réussi",
                    content = @Content(schema = @Schema(implementation = GenericApiResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requête mal formée (ex: token invalide)",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Non autorisé (token expiré ou manquant)",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Accès interdit",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Erreur technique inattendue",
                    content = @Content(schema = @Schema(implementation = GenericApiErrorResponse.class)))
    })
    @PostMapping(value = REFRESH)
    public ResponseEntity<GenericApiResponse<TokenResponseDto>> refresh(
            @RequestHeader("refresh_token") String authHeader
    ) {

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new GenericApiResponse<TokenResponseDto>(
                        HttpStatus.OK,
                        HttpStatus.OK.value(),
                        "User not exist, Update is impossible",
                        BASE_AUTH + LOGIN,
                        this.keycloakService.refreshToken(authHeader))
                );

    }
}
