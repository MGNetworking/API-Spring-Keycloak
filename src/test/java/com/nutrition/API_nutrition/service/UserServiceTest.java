package com.nutrition.API_nutrition.service;

import com.nutrition.API_nutrition.model.dto.RegisterRequestDto;
import com.nutrition.API_nutrition.model.dto.UserResponseDto;
import com.nutrition.API_nutrition.model.entity.Gender;
import com.nutrition.API_nutrition.model.entity.User;
import com.nutrition.API_nutrition.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Test de création d'un utilisateur")
    public void testCreateUser_Success() {

        // Arrange
        // Créer un DTO avec toutes les données requises
        RegisterRequestDto dto = new RegisterRequestDto();
        dto.setKeycloakId("kc123456");
        dto.setEmail("test@example.com");
        dto.setFirstName("John");
        dto.setLastName("Doe");
        dto.setBirthdate(LocalDate.of(1990, 1, 1));
        dto.setGender(Gender.MALE);
        dto.setHeight((short) 180);
        dto.setWeight((short) 75);

        User savedUser = dto.UserMapping(); // Utiliser UserMapping pour créer l'objet User à retourner
        savedUser.setId(1L); // Ajouter un ID pour simuler la sauvegarde en base de données

        // Capture l'argument et configure le retour
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(userCaptor.capture())).thenReturn(savedUser);

        // Act
        UserResponseDto result = userService.createUser(dto);

        // Assert
        // Vérifier que le résultat a l'ID attendu et correspond au mapping
        assertNotNull(result);
        assertEquals(1L, result.getId());

        // Vérifier que l'objet User passé au repository correspond au mapping du DTO
        User capturedUser = userCaptor.getValue();
        assertEquals(dto.getKeycloakId(), capturedUser.getKeycloakId());
        assertEquals(dto.getEmail(), capturedUser.getEmail());
        assertEquals(dto.getFirstName(), capturedUser.getFirstName());
        assertEquals(dto.getLastName(), capturedUser.getLastName());
        assertEquals(dto.getBirthdate(), capturedUser.getBirthDate());
        assertEquals(dto.getGender(), capturedUser.getGender());
        assertEquals(dto.getHeight(), capturedUser.getHeight());
        assertEquals(dto.getWeight(), capturedUser.getWeight());


    }

}