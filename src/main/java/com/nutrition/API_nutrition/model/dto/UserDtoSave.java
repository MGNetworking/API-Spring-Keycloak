package com.nutrition.API_nutrition.model.dto;

import com.nutrition.API_nutrition.model.entity.Gender;
import com.nutrition.API_nutrition.model.entity.User;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDtoSave {

    @NotBlank(message = "KeycloakId is required")
    private String keycloakId;

    @Email(message = "Adresse email invalide")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Firstname is required")
    private String firstname;

    @NotBlank(message = "Lastname is required")
    private String lastname;

    @Past(message = "Date of birth is invalid")
    private LocalDate birthdate;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @NotNull(message = "Size is required")
    @Positive(message = "The height is invalid")
    private Float height; // en cm

    @NotNull(message = "Weight is required")
    @Positive(message = "The weight is invalid")
    private Float weight; // en kg

    public User mapping(){
        User user = new User();
        user.setKeycloakId(this.keycloakId);
        user.setEmail(this.email);
        user.setFirstName(this.firstname);
        user.setLastName(this.lastname);
        user.setBirthDate(this.birthdate);
        user.setGender(this.gender);
        user.setHeight(this.height);
        user.setWeight(this.weight);
        return user;
    }
}
