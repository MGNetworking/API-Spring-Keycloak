package com.nutrition.API_nutrition.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutrition.API_nutrition.model.dto.RegisterRequestDto;
import com.nutrition.API_nutrition.model.dto.TokenResponseDto;
import com.nutrition.API_nutrition.model.dto.UserResponseDto;
import com.nutrition.API_nutrition.model.entity.Gender;
import com.nutrition.API_nutrition.model.entity.User;
import com.nutrition.API_nutrition.service.KeycloakService;
import com.nutrition.API_nutrition.service.UserService;
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
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Test
    @DisplayName("Devrait créer un utilisateur avec succès quand les données sont valides")
    void shouldCreateUserSuccessfullyWhenDataIsValid() throws Exception {

        RegisterRequestDto requestDto = new RegisterRequestDto();
        requestDto = new RegisterRequestDto();
        requestDto.setUserName("Username");
        requestDto.setFirstName("Firstname");
        requestDto.setLastName("Lastname");
        requestDto.setPassword("super-secret");
        requestDto.setBirthdate(LocalDate.parse("2000-01-15"));
        requestDto.setEmail("test@example.com");
        requestDto.setGender(Gender.MALE);
        requestDto.setHeight((short) 180);
        requestDto.setWeight((byte) 80);

        User user = requestDto.UserMapping();
        UserResponseDto userResponse = new UserResponseDto().mappingToUser(user);

        TokenResponseDto token = new TokenResponseDto();
        token.setAccessToken("mocked-jwt");

        // mocks
        when(keycloakService.userExistsById("test-keycloak-id")).thenReturn(false);
        when(userService.createUser(any())).thenReturn(userResponse);
        when(keycloakService.login("Username", "super-secret")).thenReturn(token);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Authorization", "Bearer mocked-jwt"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.data.userName").value("Username"));

    }
}