package com.nutrition.API_nutrition.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutrition.API_nutrition.model.dto.RegisterRequestDto;
import com.nutrition.API_nutrition.model.dto.TokenResponseDto;
import com.nutrition.API_nutrition.model.dto.UserResponseDto;
import com.nutrition.API_nutrition.model.entity.Gender;
import com.nutrition.API_nutrition.model.entity.User;
import com.nutrition.API_nutrition.security.AccessKeycloak;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UsersController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
        "spring.sql.init.mode=never"
})
class UsersControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private KeycloakService keycloakService;

    @MockitoBean
    private AccessKeycloak accessKeycloak;

    RegisterRequestDto requestDto;

    @BeforeEach
    public void setUp() {
        this.requestDto = new RegisterRequestDto();
        requestDto.getKeycloakUserData().setUserName("Username");
        requestDto.getKeycloakUserData().setFirstName("Firstname");
        requestDto.getKeycloakUserData().setLastName("Lastname");
        requestDto.getKeycloakUserData().setPassword("new-secret");
        requestDto.getKeycloakUserData().setEmail("newemail@example.com");
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

        when(keycloakService.checkUserExist(any(RegisterRequestDto.class))).thenReturn(false);
        when(userService.createUser(this.requestDto)).thenReturn(userResponse);
        when(keycloakService.login(
                this.requestDto.getKeycloakUserData().getUserName(),
                this.requestDto.getKeycloakUserData().getPassword())).thenReturn(token);

        // Act & Assert
        String uri = UsersController.BASE_USERS + UsersController.REGISTER;
        mockMvc.perform(post(uri)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer mocked-jwt")
                        .content(this.objectMapper.writeValueAsString(this.requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.data.userResponseDto.userName").value("Username"))
                .andExpect(jsonPath("$.data.token.accessToken").value("mocked-jwt"));
    }

    @Test
    @DisplayName("Devrait mettre à jour un utilisateur avec succès")
    void shouldUpdateUserSuccessfully() throws Exception {

        // Arrange
        this.requestDto.getKeycloakUserData().setKeycloakId("keycloak-id");
        UserResponseDto updatedUser = new UserResponseDto()
                .mappingToUser(this.requestDto.UserMapping());

        String authHeaderMock = "Bearer fake-token-for-testing";
        String tokenMock = "fake-token-for-testing";
        TokenResponseDto responseDto = new TokenResponseDto();
        responseDto.setAccessToken(tokenMock);

        // Mocks
        when(keycloakService.checkUserExist(this.requestDto)).thenReturn(false);
        when(userService.updateUser(this.requestDto)).thenReturn(updatedUser);
        when(this.keycloakService.refreshToken(anyString())).thenReturn(responseDto);
        when(accessKeycloak.extractToken(authHeaderMock)).thenReturn(tokenMock);

        // Act & Assert
        String uri = UsersController.BASE_USERS + UsersController.UPDATE_USER;
        mockMvc.perform(put(uri)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeaderMock)
                        .content(objectMapper.writeValueAsString(this.requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data.userResponseDto.userName").value("Username"))
                .andExpect(jsonPath("$.data.token.accessToken").value(tokenMock));
    }

    @Test
    @DisplayName("Devrait supprimer un utilisateur avec succès")
    void shouldDeleteUserSuccessfully() throws Exception {

        // Arrange
        String userId = "test-user-id";
        when(keycloakService.checkUserExist(any(RegisterRequestDto.class)))
                .thenReturn(false);
        doNothing().when(userService).deleteUser(userId);

        // Act & Assert
        String uri = UsersController.BASE_USERS + UsersController.DELETE_USER;
        mockMvc.perform(delete(uri, userId))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$.status").value("NO_CONTENT"));
    }

    @Test
    @DisplayName("Devrait récupérer un utilisateur avec succès")
    void shouldGetUserSuccessfully() throws Exception {

        // Arrange
        String userId = "test-user-id";
        when(keycloakService.checkUserExist(any(RegisterRequestDto.class)))
                .thenReturn(false);
        when(userService.getuser(userId))
                .thenReturn(Optional.of(this.requestDto.UserMapping()));

        // Act & Assert
        String uri = UsersController.BASE_USERS + UsersController.GET_USER_ID;
        mockMvc.perform(get(uri, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data.userName").value("Username"));
    }


}