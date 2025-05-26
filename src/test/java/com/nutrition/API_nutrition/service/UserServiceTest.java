package com.nutrition.API_nutrition.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nutrition.API_nutrition.exception.ApiException;
import com.nutrition.API_nutrition.exception.ErrorCode;
import com.nutrition.API_nutrition.model.dto.RegisterRequestDto;
import com.nutrition.API_nutrition.model.dto.UserResponseDto;
import com.nutrition.API_nutrition.model.entity.Gender;
import com.nutrition.API_nutrition.model.entity.User;
import com.nutrition.API_nutrition.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.representations.idm.RoleRepresentation;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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
        dto.getKeycloakUserData().setKeycloakId("kc123456");
        dto.getKeycloakUserData().setPassword("password");
        dto.getKeycloakUserData().setUserName("UserName");
        dto.getKeycloakUserData().setFirstName("FirstName");
        dto.getKeycloakUserData().setLastName("LastName");
        dto.getKeycloakUserData().setEmail("FirstName.LastName@example.com");
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

        List<RoleRepresentation> roleRepresentations = new ArrayList<>();
        RoleRepresentation roleUser = new RoleRepresentation(
                "ROLE_USER", "Role user test", true);

        doNothing().when(keycloakService).createUser(any());
        when(keycloakService.getClientScopedRoles()).thenReturn(List.of(roleUser));
        doNothing().when(keycloakService).addUserRolesClient(
                this.dto.getKeycloakUserData().getKeycloakId(), List.of(roleUser));

        // Simuler la sauvegarde en base
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(userCaptor.capture())).thenReturn(dto.UserMapping());

        // Act : lancement de la méthode à tester
        UserResponseDto result = userService.createUser(dto);

        // Assert : Vérifier des résultats
        assertNotNull(result);

        // Vérifier le retour capturé
        User capturedUser = userCaptor.getValue();
        assertEquals(dto.getKeycloakUserData().getKeycloakId(), capturedUser.getKeycloakId());
        assertEquals(dto.getKeycloakUserData().getEmail(), capturedUser.getEmail());
        assertEquals(dto.getKeycloakUserData().getFirstName(), capturedUser.getFirstName());
        assertEquals(dto.getKeycloakUserData().getLastName(), capturedUser.getLastName());
        assertEquals(dto.getBirthdate(), capturedUser.getBirthDate());
        assertEquals(dto.getGender(), capturedUser.getGender());
        assertEquals(dto.getHeight(), capturedUser.getHeight());
        assertEquals(dto.getWeight(), capturedUser.getWeight());

        // Vérifie que les méthodes void ont bien été appelées
        verify(keycloakService).createUser(any());
        verify(keycloakService).addUserRolesClient(dto.getKeycloakUserData().getKeycloakId(), List.of(roleUser));
    }

    @Test
    @DisplayName("Test d'échec lors de la création d'un utilisateur")
    public void testCreateUser_Failure() {
        // Arrange
        // Simuler une exception dans la methode void
        doThrow(new ApiException("create user",
                HttpStatus.BAD_REQUEST,
                ErrorCode.USER_CREATION_FAILED.toString()))
                .when(keycloakService).createUser(dto);

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class, () -> {
            userService.createUser(this.dto);
        });

        // Vérifie que le message d'erreur contient la chaîne attendue
        assertEquals(ErrorCode.USER_CREATION_FAILED.toString(), exception.getErrorCode());

        // Vérifie que la méthode de sauvegarde en base n'a pas été appelée
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test de création d'un utilisateur avec DTO null")
    public void testCreateUser_NullDto() {
        // Act & Assert
        ApiException exception = assertThrows(ApiException.class, () -> {
            userService.createUser(null);
        });

        assertEquals(ErrorCode.DB_ERROR.toString(), exception.getErrorCode());

        // Vérifier qu'aucune méthode n'a été appelée
        verifyNoInteractions(keycloakService);
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Test de récupération d'un utilisateur")
    public void testGetUser_Success() {
        // Arrange
        String keycloakId = "kc123456";
        User user = dto.UserMapping();
        when(userRepository.findByKeycloakId(keycloakId))
                .thenReturn(Optional.of(user));

        // Act
        Optional<User> result = userService.getuser(keycloakId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(keycloakId, result.get().getKeycloakId());
        verify(userRepository).findByKeycloakId(keycloakId);
    }

    @Test
    @DisplayName("Test d'échec lors de la récupération d'un utilisateur")
    public void testGetUser_Failure() {
        // Arrange
        String keycloakId = "nonexistent";
        when(userRepository.findByKeycloakId(keycloakId))
                .thenThrow(new ApiException("Failed to research user",
                        HttpStatus.BAD_REQUEST,
                        ErrorCode.USER_RESEARCH_FAILED.toString()));

        // Act & Assert
        Exception exception = assertThrows(ApiException.class, () -> {
            userService.getuser(keycloakId);
        });

        assertEquals("Failed to research user", exception.getMessage());
        verify(userRepository).findByKeycloakId(keycloakId);
    }


    @Test
    @DisplayName("Test de mise à jour d'un utilisateur")
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

        assertEquals(dto.getKeycloakUserData().getKeycloakId(), resutltatUser.getKeycloakId());
        assertEquals(dto.getKeycloakUserData().getUserName(), resutltatUser.getUsername());
        assertEquals(dto.getKeycloakUserData().getFirstName(), resutltatUser.getFirstName());
        assertEquals(dto.getKeycloakUserData().getLastName(), resutltatUser.getLastName());
        assertEquals(dto.getKeycloakUserData().getEmail(), resutltatUser.getEmail());
    }

    @Test
    @DisplayName("Test d'échec lors de la mise à jour d'un utilisateur")
    public void testUpdateUser_Failure() {

        // Arrange
        when(this.keycloakService.updateUser(dto)).thenReturn(false);

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class, () -> {
            this.userService.updateUser(this.dto);
        });

        assertEquals(ErrorCode.USER_UPDATE_FAILED.toString(), exception.getErrorCode());

        // Vérifie que la méthode de sauvegarde en base n'a pas été appelée
        verify(this.userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test de mise à jour d'un utilisateur avec DTO null")
    public void testUpdateUser_NullDto() {
        // Act & Assert
        ApiException exception = assertThrows(ApiException.class, () -> {
            userService.updateUser(null);
        });

        assertEquals(ErrorCode.USER_RESEARCH_FAILED.toString(), exception.getErrorCode());

        // Vérifier qu'aucune méthode n'a été appelée
        verifyNoInteractions(keycloakService);
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Test de suppression d'un utilisateur")
    public void testDeleteUser_Success() {
        // Arrange
        String keycloakId = "kc123456";
        when(keycloakService.removeUser(keycloakId)).thenReturn(true);
        doNothing().when(userRepository).deleteById(keycloakId);
        when(userRepository.existsById(keycloakId)).thenReturn(false);

        // Act
        userService.deleteUser(keycloakId);

        // Assert
        verify(keycloakService).removeUser(keycloakId);
        verify(userRepository).deleteById(keycloakId);
        verify(userRepository).existsById(keycloakId);
    }

    @Test
    @DisplayName("Test d'échec lors de la suppression d'un utilisateur - échec Keycloak")
    public void testDeleteUser_KeycloakFailure() {
        // Arrange
        String keycloakId = "kc123456";
        when(keycloakService.removeUser(keycloakId)).thenReturn(false);

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class, () -> {
            userService.deleteUser(keycloakId);
        });

        assertEquals(ErrorCode.KEYCLOAK_BAD_REQUEST.toString(), exception.getErrorCode());

        // Vérifier que la méthode de suppression en base n'a pas été appelée
        verify(userRepository, never()).deleteById(anyString());
    }

    @Test
    @DisplayName("Test d'échec lors de la suppression d'un utilisateur - utilisateur toujours présent en base")
    public void testDeleteUser_DatabaseFailure() {
        // Arrange
        String keycloakId = "kc123456";
        when(keycloakService.removeUser(keycloakId)).thenReturn(true);
        doNothing().when(userRepository).deleteById(keycloakId);
        when(userRepository.existsById(keycloakId)).thenReturn(true);

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class, () -> {
            userService.deleteUser(keycloakId);
        });

        assertEquals(ErrorCode.DB_ERROR.toString(), exception.getErrorCode());

        // Vérifier que les méthodes ont été appelées
        verify(keycloakService).removeUser(keycloakId);
        verify(userRepository).deleteById(keycloakId);
    }

    @Test
    @DisplayName("Test de suppression d'un utilisateur avec ID null")
    public void testDeleteUser_NullId() {
        // Act & Assert
        ApiException exception = assertThrows(ApiException.class, () -> {
            userService.deleteUser(null);
        });

        assertEquals(ErrorCode.BAD_REQUEST_PARAMETER.toString(), exception.getErrorCode());

        // Vérifier qu'aucune méthode n'a été appelée
        verifyNoInteractions(keycloakService);
        verifyNoInteractions(userRepository);
    }
}