package com.nutrition.API_nutrition.controller;

import com.nutrition.API_nutrition.model.dto.ApiResponseData;
import com.nutrition.API_nutrition.model.dto.UserCreatedResponseDto;
import com.nutrition.API_nutrition.model.dto.UserInputDTO;
import com.nutrition.API_nutrition.model.dto.UserOutputDto;
import com.nutrition.API_nutrition.model.entity.ActivityLevel;
import com.nutrition.API_nutrition.model.entity.Gender;
import com.nutrition.API_nutrition.model.entity.Goal;
import com.nutrition.API_nutrition.model.entity.User;
import com.nutrition.API_nutrition.model.response.GenericApiResponse;
import com.nutrition.API_nutrition.security.AccessKeycloak;
import com.nutrition.API_nutrition.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsersControllerTest {

    @InjectMocks
    private UsersController usersController;

    @Mock
    private UserService userService;

    @Mock
    private AccessKeycloak accessKeycloak;

    UserInputDTO requestDto;

    @BeforeEach
    public void setUp() {
        this.requestDto = new UserInputDTO();
        requestDto.setBirthdate(LocalDate.parse("2000-01-15"));
        requestDto.setGender(Gender.MALE);
        requestDto.setHeight((short) 180);
        requestDto.setWeight((byte) 80);
    }

    @Test
    void postUser_shouldCreateUserAndReturnCreatedResponse() {
        // Arrange
        String mockUserId = "mock-user-id";
        when(accessKeycloak.getUserIdFromToken()).thenReturn(mockUserId);

        // Act
        ResponseEntity<GenericApiResponse<ApiResponseData>> response = usersController.postUser();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        GenericApiResponse<ApiResponseData> body = response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.CREATED, body.getStatus());
        assertEquals(HttpStatus.CREATED.value(), body.getStatusCode());
        assertEquals("The user was Successfully create", body.getMessage());
        assertEquals("/api/v1/users/register", body.getPath());

        assertInstanceOf(UserCreatedResponseDto.class, body.getData());
        UserCreatedResponseDto data = (UserCreatedResponseDto) body.getData();
        assertEquals(mockUserId, data.getUserId());
        assertEquals("new user create", data.getMessage());

        // Vérifie que la méthode createUser a bien été appelée avec le bon ID
        verify(userService, times(1)).createUser(mockUserId);
    }

    @Test
    void deleteUser_shouldReturnNoContent_whenUserIsDeleted() {
        // GIVEN
        String userId = "user-123";

        // WHEN
        ResponseEntity<GenericApiResponse<String>> response = usersController.deleteUser(userId);

        // THEN
        assertNotNull(response.getBody());
        assertThat(response.getBody()).isNotNull();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
        assertThat(response.getBody().getMessage()).isEqualTo("This user is delete with successfully");
        assertThat(response.getBody().getData()).isNull();
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/users/" + userId);

        // VERIFY
        verify(userService).deleteUser(userId);
    }

    @Test
    void getUser_shouldReturnUserOutputDto_whenUserExists() {
        // GIVEN
        String userId = "user-123";
        UserOutputDto userOutputDto = new UserOutputDto();
        userOutputDto.keycloakId = userId;
        userOutputDto.birthdate = LocalDate.of(1990, 1, 1);
        userOutputDto.height = 180;
        userOutputDto.weight = 75;
        userOutputDto.setGender(Gender.MALE);
        userOutputDto.setActivityLevel(ActivityLevel.MODERATELY_ACTIVE);
        userOutputDto.setGoal(Goal.MAINTENANCE);
        userOutputDto.setAllergies(List.of("Peanuts", "Shellfish"));
        userOutputDto.setDietaryPreference(List.of("Vegan"));

        when(userService.getUser(userId)).thenReturn(userOutputDto);

        // WHEN
        ResponseEntity<GenericApiResponse<ApiResponseData>> response = usersController.getUser(userId);

        // THEN
        assertNotNull(response.getBody());
        assertThat(response.getBody()).isNotNull();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getBody().getMessage()).isEqualTo("user find successfully");
        assertThat(response.getBody().getPath()).isEqualTo(UsersController.BASE_USERS + "/" + userId);
        assertThat(response.getBody().getData()).isEqualTo(userOutputDto);

        verify(userService).getUser(userId);
    }

    @Test
    void updateUser_shouldReturnUpdatedUserResponse() {
        // GIVEN
        UserInputDTO inputDTO = new UserInputDTO();
        inputDTO.setKeycloakId("user-123");

        User updatedUser = new User();
        updatedUser.setKeycloakId("user-123");

        when(userService.updateUser(inputDTO)).thenReturn(updatedUser);

        // WHEN
        ResponseEntity<GenericApiResponse<ApiResponseData>> response = usersController.updateUser(inputDTO);

        // THEN
        assertNotNull(response.getBody());
        assertThat(response.getBody()).isNotNull();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getBody().getMessage()).isEqualTo("This user is update with successfully");
        assertThat(response.getBody().getPath()).isEqualTo(UsersController.BASE_USERS + UsersController.UPDATE_USER);

        assertThat(response.getBody().getData()).isInstanceOf(UserCreatedResponseDto.class);
        UserCreatedResponseDto responseData = (UserCreatedResponseDto) response.getBody().getData();
        assertThat(responseData.getUserId()).isEqualTo(updatedUser.getKeycloakId());
        assertThat(responseData.getMessage()).isEqualTo("User created");

        verify(userService).updateUser(inputDTO);
    }

}