package com.nutrition.API_nutrition.model.dto;

import com.nutrition.API_nutrition.model.entity.Gender;
import com.nutrition.API_nutrition.model.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
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

    @Schema(description = "Keycloak user identifier", example = "123e4567-e89b-12d3-a456-426614174000", required = false)
    private String keycloakId;

    @Schema(description = "Username (avatar name)", example = "johndoe", required = true)
    @NotBlank(message = "Username is required")
    private String userName;

    @Schema(description = "First name", example = "John", required = true)
    @NotBlank(message = "First name is required")
    private String firstName;

    @Schema(description = "Last name", example = "Doe", required = true)
    @NotBlank(message = "Last name is required")
    private String lastName;

    @Schema(description = "Password", example = "P@ssw0rd123", required = true)
    @NotBlank(message = "Password is required")
    private String password;

    @Schema(description = "Email address", example = "johndoe.dupont@gmail.com", required = true)
    @Email(message = "Invalid email address")
    @NotBlank(message = "Email is required")
    private String email;

    @Schema(description = "Date of birth (format: yyyy-MM-dd)", example = "1990-05-15", required = true)
    @Past(message = "Date of birth must be in the past")
    private LocalDate birthdate;

    @Schema(description = "User gender (MALE, FEMALE, or OTHER)", example = "MALE", required = true)
    @NotNull(message = "Gender is required")
    private Gender gender;

    @Schema(description = "Height in centimeters", example = "180", required = true)
    @NotNull(message = "Height is required")
    @Positive(message = "Height must be a positive number")
    private short height;

    @Schema(description = "Weight in kilograms", example = "75", required = true)
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
        user.setKeycloakId(this.keycloakId);
        user.setEmail(this.email);
        user.setUsername(this.userName);
        user.setFirstName(this.firstName);
        user.setLastName(this.lastName);
        user.setBirthDate(this.birthdate);
        user.setGender(this.gender);
        user.setHeight(this.height);
        user.setWeight(this.weight);
        return user;
    }
}
