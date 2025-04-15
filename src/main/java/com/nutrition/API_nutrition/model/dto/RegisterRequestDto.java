package com.nutrition.API_nutrition.model.dto;

import com.nutrition.API_nutrition.model.entity.Gender;
import com.nutrition.API_nutrition.model.entity.User;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Classe de création d'un utilisateur
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequestDto {

    //@NotBlank(message = "KeycloakId is required")
    private String keycloakId;

    @NotBlank(message = "Username is required")
    private String userName;                    // Nom d'utilisateur

    @NotBlank(message = "Firstname is required")
    private String firstName;                   // Prénom

    @NotBlank(message = "Lastname is required")
    private String lastName;                    // Nom de famille

    @NotBlank(message = "password is required")
    private String password;

    @Email(message = "Adresse email invalide")
    @NotBlank(message = "Email is required")
    private String email;

    @Past(message = "Date of birth is invalid")
    private LocalDate birthdate;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @NotNull(message = "Size is required")
    @Positive(message = "The height is invalid")
    private short height; // en cm

    @NotNull(message = "Weight is required")
    @Positive(message = "The weight is invalid")
    private short weight; // en kg

    /**
     * Mapping vers entity
     *
     * @return User entity
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
