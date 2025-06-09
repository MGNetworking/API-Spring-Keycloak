package com.nutrition.API_nutrition.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nutrition.API_nutrition.config.UserFactory;
import com.nutrition.API_nutrition.exception.ApiException;
import com.nutrition.API_nutrition.exception.ErrorCode;
import com.nutrition.API_nutrition.model.dto.UserInputDTO;
import com.nutrition.API_nutrition.model.dto.UserOutputDto;
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
import org.springframework.dao.*;
import org.springframework.http.HttpStatus;
import org.springframework.orm.jpa.JpaSystemException;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
    private UserFactory userFactory;

    UserInputDTO userInputDTO;
    UserOutputDto userOutputDto;

    @BeforeEach
    public void init() {

        this.userInputDTO = new UserInputDTO();
        userInputDTO.setKeycloakId("kc123456");
        userInputDTO.setBirthdate(LocalDate.of(1990, 1, 1));
        userInputDTO.setGender(Gender.MALE);
        userInputDTO.setHeight((short) 180);
        userInputDTO.setWeight((short) 75);

        this.userOutputDto = new UserOutputDto();
        userOutputDto.setKeycloakId(this.userInputDTO.getKeycloakId());
        userOutputDto.setBirthdate(LocalDate.of(1990, 1, 1));
        userOutputDto.setGender(Gender.MALE);
        userOutputDto.setHeight((short) 180);
        userOutputDto.setWeight((short) 75);
    }

    @Test
    @DisplayName("Devrait créer un utilisateur avec succès")
    public void shouldCreateUserSuccessfully() throws JsonProcessingException {

        // Arrange
        String userId = this.userInputDTO.getKeycloakId();
        User expectedUser = new User();
        expectedUser.setKeycloakId(userId);

        // Mock
        when(userRepository.save(any(User.class))).thenReturn(expectedUser);

        // When
        assertDoesNotThrow(() -> userService.createUser(userId));

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        assertEquals(userId, userCaptor.getValue().getKeycloakId());

    }

    @Test
    @DisplayName("Devrait mettre à jour l'utilisateur avec succè")
    void shouldUpdateUserSuccessfully() {
        // Given
        String keycloakId = this.userInputDTO.getKeycloakId();

        User existingUser = new User();
        existingUser.setKeycloakId(keycloakId);

        User updatedUser = new User();
        updatedUser.setKeycloakId(keycloakId);
        updatedUser.setWeight((short) 100);

        when(userRepository.findById(keycloakId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(existingUser)).thenReturn(updatedUser);

        // When
        User result = userService.updateUser(userInputDTO);

        // Then
        assertNotNull(result);
        assertEquals(updatedUser.getWeight(), result.getWeight());

        verify(userFactory).updateUserFromDto(existingUser, userInputDTO);
        verify(userRepository).save(existingUser);
        verify(userRepository).flush();
    }

    @Test
    @DisplayName("Devrait lancer une exception lorsque l'objet est nul")
    void shouldThrowExceptionWhenDtoIsNull() {
        // Given
        UserInputDTO dto = null;

        // When + Then
        ApiException ex = assertThrows(ApiException.class, () -> userService.updateUser(dto));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        assertEquals("User data is missing or invalid", ex.getMessage());
        assertEquals(ErrorCode.INVALID_USER_DATA.toString(), ex.getErrorCode());

        verifyNoInteractions(userRepository);
        verifyNoInteractions(userFactory);
    }

    @Test
    @DisplayName("Devrait lever une exception lorsque KeycloakId est vide")
    void shouldThrowExceptionWhenKeycloakIdIsBlank() {
        // Given
        UserInputDTO dto = new UserInputDTO();
        dto.setKeycloakId("  ");

        // When + Then
        ApiException ex = assertThrows(ApiException.class, () -> userService.updateUser(dto));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        assertEquals("User data is missing or invalid", ex.getMessage());
        assertEquals(ErrorCode.INVALID_USER_DATA.toString(), ex.getErrorCode());

        verifyNoInteractions(userRepository);
        verifyNoInteractions(userFactory);
    }

    @Test
    @DisplayName("Devrait lever une exception lorsque l'utilisateur n'est pas trouvé")
    void shouldThrowExceptionWhenUserNotFound() {
        // Given
        String keycloakId = "unknown-id";
        UserInputDTO dto = new UserInputDTO();
        dto.setKeycloakId(keycloakId);

        when(userRepository.findById(keycloakId)).thenReturn(Optional.empty());

        // When + Then
        ApiException ex = assertThrows(ApiException.class, () -> userService.updateUser(dto));
        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        assertEquals("User not found, update is impossible", ex.getMessage());
        assertEquals(ErrorCode.DB_USER_NOT_FOUND.toString(), ex.getErrorCode());

        verify(userRepository).findById(keycloakId);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(userFactory);
    }

    @Test
    @DisplayName("Devrait lancer une exception en cas de violation de l'intégrité")
    void shouldThrowExceptionWhenIntegrityViolationOccurs() {
        // Given
        String keycloakId = "keycloak-123";
        UserInputDTO dto = new UserInputDTO();
        dto.setKeycloakId(keycloakId);

        User user = new User();
        user.setKeycloakId(keycloakId);

        when(userRepository.findById(keycloakId)).thenReturn(Optional.of(user));
        doNothing().when(userFactory).updateUserFromDto(user, dto);
        when(userRepository.save(user)).thenThrow(new DataIntegrityViolationException("Duplicate key"));

        // When + Then
        ApiException ex = assertThrows(ApiException.class, () -> userService.updateUser(dto));
        assertEquals(HttpStatus.CONFLICT, ex.getHttpStatus());
        assertEquals("The update failed: some of the data did not comply with the expected constraints", ex.getMessage());
        assertEquals(ErrorCode.DB_CONSTRAINT_VIOLATION.toString(), ex.getErrorCode());

        verify(userRepository).findById(keycloakId);
        verify(userFactory).updateUserFromDto(user, dto);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Devrait lancer une exception en cas d'erreur Jpa")
    void shouldThrowExceptionWhenJpaErrorOccurs() {
        // Given
        String keycloakId = "keycloak-123";
        UserInputDTO dto = new UserInputDTO();
        dto.setKeycloakId(keycloakId);

        User user = new User();
        user.setKeycloakId(keycloakId);

        when(userRepository.findById(keycloakId)).thenReturn(Optional.of(user));
        doNothing().when(userFactory).updateUserFromDto(user, dto);
        when(userRepository.save(user)).thenThrow(new JpaSystemException(new RuntimeException("JPA error")));

        // When + Then
        ApiException ex = assertThrows(ApiException.class, () -> userService.updateUser(dto));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getHttpStatus());
        assertEquals("A technical error has occurred during the update. Please try again later.", ex.getMessage());
        assertEquals(ErrorCode.DB_ERROR.toString(), ex.getErrorCode());
    }

    @Test
    @DisplayName("Devrait lancer une exception en cas d'échec de l'accès aux données")
    void shouldThrowExceptionWhenDataAccessFails() {
        // Given
        String keycloakId = "keycloak-123";
        UserInputDTO dto = new UserInputDTO();
        dto.setKeycloakId(keycloakId);

        User user = new User();
        user.setKeycloakId(keycloakId);

        when(userRepository.findById(keycloakId)).thenReturn(Optional.of(user));
        doNothing().when(userFactory).updateUserFromDto(user, dto);
        when(userRepository.save(user)).thenThrow(new DataAccessResourceFailureException("DB down"));

        // When + Then
        ApiException ex = assertThrows(ApiException.class, () -> userService.updateUser(dto));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getHttpStatus());
        assertEquals("Unable to update user data due to database access problem.", ex.getMessage());
        assertEquals(ErrorCode.DB_ERROR.toString(), ex.getErrorCode());
    }

    @Test
    @DisplayName("Devrait lancer une exception en cas d'erreur inattendue")
    void shouldThrowExceptionWhenUnexpectedErrorOccurs() {
        // Given
        String keycloakId = "keycloak-123";
        UserInputDTO dto = new UserInputDTO();
        dto.setKeycloakId(keycloakId);

        User user = new User();
        user.setKeycloakId(keycloakId);

        when(userRepository.findById(keycloakId)).thenReturn(Optional.of(user));
        doNothing().when(userFactory).updateUserFromDto(user, dto);
        when(userRepository.save(user)).thenThrow(new RuntimeException("Unknown exception"));

        // When + Then
        ApiException ex = assertThrows(ApiException.class, () -> userService.updateUser(dto));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getHttpStatus());
        assertEquals("An unexpected error has occurred during the update. Please contact support.", ex.getMessage());
        assertEquals(ErrorCode.TECHNICAL_ERROR.toString(), ex.getErrorCode());
    }

    @Test
    @DisplayName("Devrait lever une exception lorsque KeycloakId est nul")
    void shouldThrowExceptionWhenKeycloakIdIsNull() {
        ApiException ex = assertThrows(ApiException.class, () -> userService.deleteUser(null));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        assertEquals("KeycloakId cannot be null or empty", ex.getMessage());
        assertEquals(ErrorCode.BAD_REQUEST_PARAMETER.toString(), ex.getErrorCode());

        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Devrait lever une exception lorsque KeycloakId est vide")
    void shouldThrowExceptionWhenKeycloakIdIsEmpty() {
        ApiException ex = assertThrows(ApiException.class, () -> userService.deleteUser(""));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        assertEquals("KeycloakId cannot be null or empty", ex.getMessage());
        assertEquals(ErrorCode.BAD_REQUEST_PARAMETER.toString(), ex.getErrorCode());

        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Devrait lever une exception lorsque la suppression d'un utilisateur n'est pas trouvée")
    void shouldThrowExceptionWhenDeleteUserUserNotFound() {
        String keycloakId = "non-existent-id";

        doThrow(new EmptyResultDataAccessException(1))
                .when(userRepository).deleteById(keycloakId);

        ApiException ex = assertThrows(ApiException.class, () -> userService.deleteUser(keycloakId));

        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        assertEquals("User not found for deletion", ex.getMessage());
        assertEquals(ErrorCode.DB_USER_NOT_FOUND.toString(), ex.getErrorCode());

        verify(userRepository).deleteById(keycloakId);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("Devrait lever une exception en cas de violation de l'intégrité de l'utilisateur supprimé")
    void shouldThrowExceptionWhenDeleteUserIntegrityViolationOccurs() {
        String keycloakId = "user-123";

        doThrow(new DataIntegrityViolationException("Foreign key violation"))
                .when(userRepository).deleteById(keycloakId);

        ApiException ex = assertThrows(ApiException.class, () -> userService.deleteUser(keycloakId));

        assertEquals(HttpStatus.CONFLICT, ex.getHttpStatus());
        assertEquals("Cannot delete user due to database constraints", ex.getMessage());
        assertEquals(ErrorCode.DB_CONSTRAINT_VIOLATION.toString(), ex.getErrorCode());

        verify(userRepository).deleteById(keycloakId);
    }

    @Test
    @DisplayName("devrait lancer une exception en cas d'erreur lors de la suppression d'un utilisateur Jpa")
    void shouldThrowExceptionWhenDeleteUserJpaErrorOccurs() {
        String keycloakId = "user-123";

        doThrow(new JpaSystemException(new RuntimeException("JPA failure")))
                .when(userRepository).deleteById(keycloakId);

        ApiException ex = assertThrows(ApiException.class, () -> userService.deleteUser(keycloakId));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getHttpStatus());
        assertEquals("Error during user deletion transaction", ex.getMessage());
        assertEquals(ErrorCode.DB_ERROR.toString(), ex.getErrorCode());
    }

    @Test
    @DisplayName("Devrait lancer une exception en cas d'erreur inattendue lors de la suppression d'un utilisateur")
    void shouldThrowExceptionWhenDeleteUserUnexpectedErrorOccurs() {
        String keycloakId = "user-123";

        doThrow(new RuntimeException("Unexpected failure"))
                .when(userRepository).deleteById(keycloakId);

        ApiException ex = assertThrows(ApiException.class, () -> userService.deleteUser(keycloakId));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getHttpStatus());
        assertEquals("Failed to delete user due to unexpected error", ex.getMessage());
        assertEquals(ErrorCode.TECHNICAL_ERROR.toString(), ex.getErrorCode());
    }

    @Test
    void getUser_whenUserExists_shouldReturnUserOutputDto() {
        User userEntity = new User();
        String userId = this.userInputDTO.getKeycloakId();


        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
        when(userFactory.userToUserDtoOutput(userEntity)).thenReturn(userOutputDto);

        UserOutputDto result = userService.getUser(userId);

        assertThat(result).isNotNull();
        assertThat(result.keycloakId).isEqualTo(userId);

        verify(userRepository).findById(userId);
        verify(userFactory).userToUserDtoOutput(userEntity);
    }

    @Test
    void getUser_whenUserNotFound_shouldThrowApiExceptionNotFound() {

        String userId = this.userInputDTO.getKeycloakId();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        ApiException thrown = assertThrows(ApiException.class, () -> userService.getUser(userId));
        assertThat(thrown.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(thrown.getMessage()).contains("User not found with id");
    }

    @Test
    void getUser_whenDataIntegrityViolationException_shouldThrowApiExceptionConflict() {

        String userId = this.userInputDTO.getKeycloakId();
        when(userRepository.findById(userId)).thenThrow(DataIntegrityViolationException.class);

        ApiException thrown = assertThrows(ApiException.class, () -> userService.getUser(userId));
        assertThat(thrown.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(thrown.getMessage()).contains("Database constraint violation");
    }

    @Test
    void getUser_whenJpaSystemException_shouldThrowApiExceptionServiceUnavailable() {

        String userId = this.userInputDTO.getKeycloakId();
        when(userRepository.findById(userId)).thenThrow(JpaSystemException.class);

        ApiException thrown = assertThrows(ApiException.class, () -> userService.getUser(userId));
        assertThat(thrown.getHttpStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(thrown.getMessage()).contains("Database system error");
    }

    @Test
    void getUser_whenTransientDataAccessException_shouldThrowApiExceptionServiceUnavailable() {

        String userId = this.userInputDTO.getKeycloakId();
        when(userRepository.findById(userId)).thenThrow(new QueryTimeoutException("Simulated transient error"));

        ApiException thrown = assertThrows(ApiException.class, () -> userService.getUser(userId));
        assertThat(thrown.getHttpStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(thrown.getMessage()).contains("Database system error");
    }

    @Test
    void getUser_whenDataAccessException_shouldThrowApiExceptionInternalServerError() {

        String userId = this.userInputDTO.getKeycloakId();
        when(userRepository.findById(userId)).thenThrow(new RecoverableDataAccessException("Simulated DB failure"));

        ApiException thrown = assertThrows(ApiException.class, () -> userService.getUser(userId));
        assertThat(thrown.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(thrown.getMessage()).contains("Database access error");
    }

    @Test
    void getUser_whenUnexpectedException_shouldThrowApiExceptionInternalServerError() {

        String userId = this.userInputDTO.getKeycloakId();
        when(userRepository.findById(userId)).thenThrow(RuntimeException.class);

        ApiException thrown = assertThrows(ApiException.class, () -> userService.getUser(userId));
        assertThat(thrown.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(thrown.getMessage()).contains("Unexpected error occurred");
    }

}
