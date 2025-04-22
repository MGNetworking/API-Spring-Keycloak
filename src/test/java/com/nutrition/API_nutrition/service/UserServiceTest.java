package com.nutrition.API_nutrition.service;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

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

    @Test
    @DisplayName("Test de création d'un utilisateur")
    public void testCreateUser_Success() throws JsonProcessingException {

        // Arrange : Créer un DTO avec toutes les données requises
        RegisterRequestDto dto = new RegisterRequestDto();
        dto.setKeycloakId("kc123456");
        dto.setUserName("UserName");
        dto.setFirstName("FirstName");
        dto.setLastName("LastName");
        dto.setEmail("FirstName.LastName@example.com");
        dto.setBirthdate(LocalDate.of(1990, 1, 1));
        dto.setGender(Gender.MALE);
        dto.setHeight((short) 180);
        dto.setWeight((short) 75);


        // Simuler les appels à KeycloakService (void methods)
        doNothing().when(keycloakService).createUser(any());
        doNothing().when(keycloakService).addUserRoles(anyString(), anyList());

        // Objet pour simuler le retour après un save in DB
        User savedUser = dto.UserMapping();

        // Simuler la sauvegarde en base
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(userCaptor.capture())).thenReturn(savedUser);

        // Act : lancement de la méthode à tester
        UserResponseDto result = userService.createUser(dto);

        // Assert : Vérifier des résultats
        assertNotNull(result);

        // Vérifier le retour
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

}