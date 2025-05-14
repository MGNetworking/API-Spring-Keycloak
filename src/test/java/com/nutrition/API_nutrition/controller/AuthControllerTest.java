package com.nutrition.API_nutrition.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutrition.API_nutrition.exception.ApiException;
import com.nutrition.API_nutrition.exception.ErrorCode;
import com.nutrition.API_nutrition.model.dto.KeycloakLoginRequestDto;
import com.nutrition.API_nutrition.model.dto.TokenResponseDto;
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

    @Test
    @DisplayName("Devrais authentifier l'utilisateur")
    void shouldBeAuthenticated_login() throws Exception {

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
    @DisplayName("Ne devrais pas pourvoir être authentifier: BAD_REQUEST")
    void ShouldBeNotAuthenticatedBecauseApiException400_login() throws Exception {
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
    @DisplayName("Ne devrais pas pourvoir être authentifier: UNAUTHORIZED")
    void ShouldBeNotAuthenticatedBecauseApiException401_login() throws Exception {
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
    @DisplayName("Ne devrais pas pourvoir être authentifier: FORBIDDEN")
    void ShouldBeNotAuthenticatedBecauseApiException403_login() throws Exception {
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
    @DisplayName("Ne devrais pas pourvoir être authentifier: INTERNAL_SERVER_ERROR")
    void ShouldBeNotAuthenticatedBecauseApiException500_login() throws Exception {
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

}