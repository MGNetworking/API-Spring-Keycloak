package com.nutrition.API_nutrition.model.dto;

import com.nutrition.API_nutrition.model.entity.Gender;
import com.nutrition.API_nutrition.model.validation.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
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
public class UserInputDTO implements ApiResponseData {

    @Schema(description = "Keycloak user identifier", example = "123e4567-e89b-12d3-a456-426614174000", required = false)
    @NotNull(message = "User id Keycloak is missing", groups = OnUpdate.class)
    public String keycloakId;

    @Schema(description = "Date of birth (format: yyyy-MM-dd)", example = "1990-05-15")
    @NotNull(message = "Birthdate is required", groups = OnUpdate.class)
    @Past(message = "Date of birth must be in the past", groups = OnUpdate.class)
    public LocalDate birthdate;

    @Schema(description = "User gender (MALE, FEMALE, or OTHER)", example = "MALE")
    @NotNull(message = "Gender is required", groups = OnUpdate.class)
    public Gender gender;

    @Schema(description = "Height in centimeters", example = "180")
    @NotNull(message = "Height is required", groups = OnUpdate.class)
    @Positive(message = "Height must be a positive number")
    private short height;

    @Schema(description = "Weight in kilograms", example = "75")
    @NotNull(message = "Weight is required", groups = OnUpdate.class)
    @Positive(message = "Weight must be a positive number")
    public short weight;


}
