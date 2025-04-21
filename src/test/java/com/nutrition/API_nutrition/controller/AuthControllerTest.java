package com.nutrition.API_nutrition.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutrition.API_nutrition.model.dto.RegisterRequestDto;
import com.nutrition.API_nutrition.model.dto.UserResponseDto;
import com.nutrition.API_nutrition.model.entity.Gender;
import com.nutrition.API_nutrition.model.entity.User;
import com.nutrition.API_nutrition.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    private RegisterRequestDto userDtoValid;
    private UserResponseDto userResponseDto;

    @BeforeEach
    void setUp() {
        // Initialiser un DTO utilisateur valide
        userDtoValid = new RegisterRequestDto();
        userDtoValid.setKeycloakId("keycloakId");
        userDtoValid.setFirstName("Firstname");
        userDtoValid.setLastName("Lastname");
        userDtoValid.setBirthdate(LocalDate.parse("2000-01-15"));
        userDtoValid.setEmail("test@example.com");
        userDtoValid.setGender(Gender.MALE);
        userDtoValid.setHeight((short) 180);
        userDtoValid.setWeight((byte) 80);

        // Initialiser un mock User pour le retour du service
        User user = userDtoValid.UserMapping();
        //user.setId(1L);
        userResponseDto.mappingToUser(user);

        // réinitialiser les mocks entre les tests
        Mockito.reset(this.userService);

    }

    @Test
    @DisplayName("Devrait créer un utilisateur avec succès quand les données sont valides")
    void shouldCreateUserSuccessfullyWhenDataIsValid() throws Exception {

        // Arrange
        Mockito.when(this.userService.createUser(ArgumentMatchers.any(RegisterRequestDto.class)))
                .thenReturn(this.userResponseDto);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDtoValid)))
                .andExpect(status().isOk())
                //.andExpect(jsonPath("$.id").value(userResponseDto.getId()))
                .andExpect(jsonPath("$.email").value(userResponseDto.getEmail()));

        // Vérifier que le service a été appelé une fois
        verify(userService, times(1))
                .createUser(ArgumentMatchers.any(RegisterRequestDto.class));

    }
}