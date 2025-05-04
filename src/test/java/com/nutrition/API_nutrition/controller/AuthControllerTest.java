package com.nutrition.API_nutrition.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutrition.API_nutrition.model.dto.RegisterRequestDto;
import com.nutrition.API_nutrition.model.dto.TokenResponseDto;
import com.nutrition.API_nutrition.model.dto.UserResponseDto;
import com.nutrition.API_nutrition.model.entity.Gender;
import com.nutrition.API_nutrition.model.entity.User;
import com.nutrition.API_nutrition.service.KeycloakService;
import com.nutrition.API_nutrition.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private KeycloakService keycloakService;

    RegisterRequestDto requestDto;

    @BeforeEach
    public void init() {
        this.requestDto = new RegisterRequestDto();
        //requestDto.setKeycloakId("keycloak-id");
        requestDto.setUserName("Username");
        requestDto.setFirstName("Firstname");
        requestDto.setLastName("Lastname");
        requestDto.setPassword("new-secret");
        requestDto.setEmail("newemail@example.com");
        requestDto.setBirthdate(LocalDate.parse("2000-01-15"));
        requestDto.setGender(Gender.MALE);
        requestDto.setHeight((short) 180);
        requestDto.setWeight((byte) 80);
    }

    @Test
    @DisplayName("Devrait créer un utilisateur avec succès quand les données sont valides")
    void shouldCreateUserSuccessfullyWhenDataIsValid() throws Exception {

        // Arrange
        User user = this.requestDto.UserMapping();
        UserResponseDto userResponse = new UserResponseDto().mappingToUser(user);

        TokenResponseDto token = new TokenResponseDto();
        token.setAccessToken("mocked-jwt");

        when(keycloakService.userExistsById("test-keycloak-id")).thenReturn(false);
        when(userService.createUser(this.requestDto)).thenReturn(userResponse);
        when(keycloakService.login(
                this.requestDto.getUserName(),
                this.requestDto.getPassword())).thenReturn(token);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(this.requestDto)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Authorization", "Bearer mocked-jwt"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.data.userName").value("Username"));

    }

    @Test
    @DisplayName("Devrait mettre à jour un utilisateur avec succès")
    void shouldUpdateUserSuccessfully() throws Exception {

        // Arrange
        this.requestDto.setKeycloakId("keycloak-id");
        UserResponseDto updatedUser = new UserResponseDto()
                .mappingToUser(this.requestDto.UserMapping());

        // Mocks
        when(keycloakService.userExistsById(this.requestDto.getKeycloakId())).thenReturn(true);
        when(userService.updateUser(this.requestDto)).thenReturn(updatedUser);

        // Act & Assert
        mockMvc.perform(put("/api/v1/auth/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(this.requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data.userName").value("Username"));
    }

    @Test
    @DisplayName("Devrait supprimer un utilisateur avec succès")
    void shouldDeleteUserSuccessfully() throws Exception {

        // Arrange
        String userId = "test-user-id";
        when(keycloakService.userExistsById(userId)).thenReturn(true);
        doNothing().when(userService).deleteUser(userId);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/auth/{userId}", userId))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$.status").value("NO_CONTENT"));
    }

    @Test
    @DisplayName("Devrait récupérer un utilisateur avec succès")
    void shouldGetUserSuccessfully() throws Exception {

        // Arrange
        String userId = "test-user-id";
        when(keycloakService.userExistsById(userId)).thenReturn(true);
        when(userService.getuser(userId)).thenReturn(Optional.of(this.requestDto.UserMapping()));

        // Act & Assert
        mockMvc.perform(get("/api/v1/auth/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data.userName").value("Username"));
    }


}