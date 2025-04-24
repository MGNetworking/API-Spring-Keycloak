package com.nutrition.API_nutrition.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nutrition.API_nutrition.model.dto.RegisterRequestDto;
import com.nutrition.API_nutrition.model.dto.UserResponseDto;
import com.nutrition.API_nutrition.model.entity.Gender;
import com.nutrition.API_nutrition.model.entity.User;
import com.nutrition.API_nutrition.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private KeycloakService keycloakService;

    RegisterRequestDto dto;

    @BeforeEach
    public void init() {

        this.dto = new RegisterRequestDto();
        dto.setKeycloakId("kc123456");
        dto.setPassword("password");
        dto.setUserName("UserName");
        dto.setFirstName("FirstName");
        dto.setLastName("LastName");
        dto.setEmail("FirstName.LastName@example.com");
        dto.setBirthdate(LocalDate.of(1990, 1, 1));
        dto.setGender(Gender.MALE);
        dto.setHeight((short) 180);
        dto.setWeight((short) 75);
    }

    @Test
    @DisplayName("Test de création d'un utilisateur")
    public void testCreateUser_Success() throws JsonProcessingException {

        // Arrange : créer les objet de simulation
        // Simuler les appels à KeycloakService (void methods)
        doNothing().when(keycloakService).createUser(any());

        doNothing().when(keycloakService).addUserRoles(anyString(), anyList());

        // Simuler la sauvegarde en base
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(userCaptor.capture())).thenReturn(dto.UserMapping());

        // Act : lancement de la méthode à tester
        UserResponseDto result = userService.createUser(dto);

        // Assert : Vérifier des résultats
        assertNotNull(result);

        // Vérifier le retour capturé
        User capturedUser = userCaptor.getValue();
        assertEquals(dto.getKeycloakId(), capturedUser.getKeycloakId());
        assertEquals(dto.getEmail(), capturedUser.getEmail());
        assertEquals(dto.getFirstName(), capturedUser.getFirstName());
        assertEquals(dto.getLastName(), capturedUser.getLastName());
        assertEquals(dto.getBirthdate(), capturedUser.getBirthDate());
        assertEquals(dto.getGender(), capturedUser.getGender());
        assertEquals(dto.getHeight(), capturedUser.getHeight());
        assertEquals(dto.getWeight(), capturedUser.getWeight());

        // Vérifie que les méthodes void ont bien été appelées
        verify(keycloakService).createUser(any());
        verify(keycloakService).addUserRoles(dto.getKeycloakId(), List.of("USER"));
    }

    @Test
    @DisplayName("test de mise à jour d'un utilisateur")
    public void testUpdateUser_Success() {

        // Arrange
        when(this.keycloakService.updateUser(any())).thenReturn(true);

        // Simuler la sauvegarde en base
        ArgumentCaptor<User> argumentCaptor = ArgumentCaptor.forClass(User.class);
        when(this.userRepository.save(argumentCaptor.capture())).thenReturn(this.dto.UserMapping());
        // simule le flush()
        doNothing().when(this.userRepository).flush();

        // Act
        UserResponseDto userResponseDto = this.userService.updateUser(this.dto);

        // Assert
        assertNotNull(userResponseDto);
        User resutltatUser = argumentCaptor.getValue();

        assertEquals(dto.getKeycloakId(), resutltatUser.getKeycloakId());
        assertEquals(dto.getUserName(), resutltatUser.getUsername());
        assertEquals(dto.getFirstName(), resutltatUser.getFirstName());
        assertEquals(dto.getLastName(), resutltatUser.getLastName());
        assertEquals(dto.getEmail(), resutltatUser.getEmail());
    }

}