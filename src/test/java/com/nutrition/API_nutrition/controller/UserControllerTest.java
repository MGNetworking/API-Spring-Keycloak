package com.nutrition.API_nutrition.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutrition.API_nutrition.model.dto.UserDtoSave;
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

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    private UserDtoSave userDtoValid;
    private User userExpected;

    @BeforeEach
    void setUp() {
        // Initialiser un DTO utilisateur valide
        userDtoValid = new UserDtoSave();
        userDtoValid.setKeycloakId("keycloakId");
        userDtoValid.setFirstname("Firstname");
        userDtoValid.setLastname("Lastname");
        userDtoValid.setBirthdate(LocalDate.parse("2000-01-15"));
        userDtoValid.setEmail("test@example.com");
        userDtoValid.setGender(Gender.MALE);
        userDtoValid.setHeight((short) 180);
        userDtoValid.setWeight((byte) 80);

        // Initialiser un mock User pour le retour du service
        userExpected = new User();
        userExpected.setId(1L);
        userDtoValid.setKeycloakId(this.userDtoValid.getKeycloakId());
        userExpected.setFirstName(this.userDtoValid.getFirstname());
        userExpected.setLastName(this.userDtoValid.getLastname());
        userExpected.setBirthDate(this.userDtoValid.getBirthdate());
        userExpected.setEmail(this.userDtoValid.getEmail());
        userDtoValid.setGender(this.userDtoValid.getGender());
        userDtoValid.setHeight(this.userDtoValid.getHeight());
        userDtoValid.setWeight(this.userDtoValid.getWeight());

        // réinitialiser les mocks entre les tests
        Mockito.reset(this.userService);


    }

    @Test
    @DisplayName("Devrait créer un utilisateur avec succès quand les données sont valides")
    void shouldCreateUserSuccessfullyWhenDataIsValid() throws Exception {

        // Arrange
        Mockito.when(userService.createUser(ArgumentMatchers.any(UserDtoSave.class)))
                .thenReturn(userExpected);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDtoValid)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userExpected.getId()))
                .andExpect(jsonPath("$.email").value(userExpected.getEmail()));

        // Vérifier que le service a été appelé une fois
        verify(userService, times(1))
                .createUser(ArgumentMatchers.any(UserDtoSave.class));

    }
}