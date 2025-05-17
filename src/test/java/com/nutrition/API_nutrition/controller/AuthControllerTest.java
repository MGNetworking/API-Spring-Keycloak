package com.nutrition.API_nutrition.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutrition.API_nutrition.exception.ApiException;
import com.nutrition.API_nutrition.exception.ErrorCode;
import com.nutrition.API_nutrition.model.dto.KeycloakLoginRequestDto;
import com.nutrition.API_nutrition.model.dto.TokenResponseDto;
import com.nutrition.API_nutrition.security.AccessKeycloak;
import com.nutrition.API_nutrition.service.KeycloakService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Classe de test unitaire des points de terminaison.
 */
@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
        "spring.sql.init.mode=never"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthController authController;

    @MockitoBean
    private KeycloakService keycloakService;

    @MockitoBean
    private AccessKeycloak accessKeycloak;

    @Test
    @DisplayName("login: Devrais authentifier l'utilisateur")
    void login_shouldBeAuthenticated() throws Exception {

        KeycloakLoginRequestDto dtoLogin = new KeycloakLoginRequestDto(
                "username",
                "password");

        TokenResponseDto token = new TokenResponseDto();
        token.setAccessToken("fake-Access-token-for-testing");
        token.setRefreshToken("fake-Refresh-token-for-testing");

        when(this.keycloakService.login(any(), any()))
                .thenReturn(token);


        // Act & Assert
        String uri = AuthController.BASE_AUTH + AuthController.LOGIN;
        mockMvc.perform(post(uri)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(this.objectMapper.writeValueAsString(dtoLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data.accessToken").value("fake-Access-token-for-testing"))
                .andExpect(jsonPath("$.data.refreshToken").value("fake-Refresh-token-for-testing"));
    }

    @Test
    @DisplayName("login: Ne devrais pas pourvoir être authentifier: BAD_REQUEST")
    void login_ShouldBeNotAuthenticatedBecauseApiException400() throws Exception {
        KeycloakLoginRequestDto dtoLogin = new KeycloakLoginRequestDto(
                "username",
                "password");

        TokenResponseDto token = new TokenResponseDto();
        token.setAccessToken("fake-Access-token-for-testing");
        token.setRefreshToken("fake-Refresh-token-for-testing");

        when(this.keycloakService.login(any(), any()))
                .thenThrow(new ApiException(
                        "Erreur message",
                        HttpStatus.BAD_REQUEST,
                        ErrorCode.KEYCLOAK_BAD_REQUEST.toString()));


        // Act & Assert
        String uri = AuthController.BASE_AUTH + AuthController.LOGIN;
        mockMvc.perform(post(uri)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(this.objectMapper.writeValueAsString(dtoLogin)))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.errorCode").value("KEYCLOAK_BAD_REQUEST"));
    }

    @Test
    @DisplayName("login: Ne devrais pas pourvoir être authentifier: UNAUTHORIZED")
    void login_ShouldBeNotAuthenticatedBecauseApiException401() throws Exception {
        KeycloakLoginRequestDto dtoLogin = new KeycloakLoginRequestDto(
                "username",
                "password");

        TokenResponseDto token = new TokenResponseDto();
        token.setAccessToken("fake-Access-token-for-testing");
        token.setRefreshToken("fake-Refresh-token-for-testing");

        when(this.keycloakService.login(any(), any()))
                .thenThrow(new ApiException(
                        "Erreur message",
                        HttpStatus.UNAUTHORIZED,
                        ErrorCode.KEYCLOAK_UNAUTHORIZED.toString()));


        // Act & Assert
        String uri = AuthController.BASE_AUTH + AuthController.LOGIN;
        mockMvc.perform(post(uri)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(this.objectMapper.writeValueAsString(dtoLogin)))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.status").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.errorCode").value("KEYCLOAK_UNAUTHORIZED"));
    }

    @Test
    @DisplayName("login: Ne devrais pas pourvoir être authentifier: FORBIDDEN")
    void login_ShouldBeNotAuthenticatedBecauseApiException403() throws Exception {
        KeycloakLoginRequestDto dtoLogin = new KeycloakLoginRequestDto(
                "username",
                "password");

        TokenResponseDto token = new TokenResponseDto();
        token.setAccessToken("fake-Access-token-for-testing");
        token.setRefreshToken("fake-Refresh-token-for-testing");

        when(this.keycloakService.login(any(), any()))
                .thenThrow(new ApiException(
                        "Erreur message",
                        HttpStatus.FORBIDDEN,
                        ErrorCode.KEYCLOAK_FORBIDDEN.toString()));


        // Act & Assert
        String uri = AuthController.BASE_AUTH + AuthController.LOGIN;
        mockMvc.perform(post(uri)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(this.objectMapper.writeValueAsString(dtoLogin)))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.status").value("FORBIDDEN"))
                .andExpect(jsonPath("$.errorCode").value("KEYCLOAK_FORBIDDEN"));
    }

    @Test
    @DisplayName("login: Ne devrais pas pourvoir être authentifier: INTERNAL_SERVER_ERROR")
    void login_ShouldBeNotAuthenticatedBecauseApiException500() throws Exception {
        KeycloakLoginRequestDto dtoLogin = new KeycloakLoginRequestDto(
                "username",
                "password");

        TokenResponseDto token = new TokenResponseDto();
        token.setAccessToken("fake-Access-token-for-testing");
        token.setRefreshToken("fake-Refresh-token-for-testing");

        when(this.keycloakService.login(any(), any()))
                .thenThrow(new ApiException(
                        "Erreur message",
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        ErrorCode.KEYCLOAK_UNEXPECTED_ERROR.toString()));

        // Act & Assert
        String uri = AuthController.BASE_AUTH + AuthController.LOGIN;
        mockMvc.perform(post(uri)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(this.objectMapper.writeValueAsString(dtoLogin)))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.status").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.errorCode").value("KEYCLOAK_UNEXPECTED_ERROR"));
    }

    @Test
    @DisplayName("logout: devrais déconnecter l'utilisateur")
    void logout_shouldBeLogoutUser() throws Exception {

        // Arrange
        when(this.accessKeycloak.isTokenValid()).thenReturn(true);
        when(this.accessKeycloak.getUserIdFromToken()).thenReturn("fake-user-Id-token");
        when(this.keycloakService.logout(any())).thenReturn(true);

        // Act & Assert
        String uri = AuthController.BASE_AUTH + AuthController.LOGOUT;
        mockMvc.perform(post(uri)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("logout: devrais retourné un message d'erreur: BAD_REQUEST")
    void logout_shouldBeNotLogoutUserApiException400() throws Exception {

        // Arrange
        when(this.accessKeycloak.isTokenValid()).thenReturn(true);
        when(this.accessKeycloak.getUserIdFromToken()).thenReturn("fake-user-Id-token");
        when(this.keycloakService.logout(any())).thenThrow(new ApiException(
                "Error message",
                HttpStatus.BAD_REQUEST,
                ErrorCode.KEYCLOAK_BAD_REQUEST.toString()));

        // Act & Assert
        String uri = AuthController.BASE_AUTH + AuthController.LOGOUT;
        mockMvc.perform(post(uri)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.errorCode").value("KEYCLOAK_BAD_REQUEST"));
    }

}