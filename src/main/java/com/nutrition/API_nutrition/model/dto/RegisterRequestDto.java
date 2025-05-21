package com.nutrition.API_nutrition.model.dto;

import com.nutrition.API_nutrition.model.entity.Gender;
import com.nutrition.API_nutrition.model.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO used for user registration
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequestDto implements ApiResponseData {

    @Valid
    @NotNull
    private KeycloakUserData keycloakUserData = new KeycloakUserData();

    @Schema(description = "Date of birth (format: yyyy-MM-dd)", example = "1990-05-15")
    @Past(message = "Date of birth must be in the past")
    private LocalDate birthdate;

    @Schema(description = "User gender (MALE, FEMALE, or OTHER)", example = "MALE")
    @NotNull(message = "Gender is required")
    private Gender gender;

    @Schema(description = "Height in centimeters", example = "180")
    @NotNull(message = "Height is required")
    @Positive(message = "Height must be a positive number")
    private short height;

    @Schema(description = "Weight in kilograms", example = "75")
    @NotNull(message = "Weight is required")
    @Positive(message = "Weight must be a positive number")
    private short weight;

    /**
     * Maps this DTO to a User entity.
     *
     * @return the corresponding User entity
     */
    public User UserMapping() {
        User user = new User();
        // User Keycloak data
        user.setKeycloakId(this.keycloakUserData.getKeycloakId());
        user.setEmail(this.keycloakUserData.getEmail());
        user.setUsername(this.keycloakUserData.getUserName());
        user.setFirstName(this.keycloakUserData.getFirstName());
        user.setLastName(this.keycloakUserData.getLastName());

        // User API data
        user.setBirthDate(this.birthdate);
        user.setGender(this.gender);
        user.setHeight(this.height);
        user.setWeight(this.weight);
        return user;
    }
}
