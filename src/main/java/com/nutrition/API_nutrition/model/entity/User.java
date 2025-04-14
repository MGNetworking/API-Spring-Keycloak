package com.nutrition.API_nutrition.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "keycloak_id", unique = true, nullable = false)
    private String keycloakId; // lien avec keycloak
    @Column(unique = true, nullable = false)
    private String email;
    @Column(name = "first_name", nullable = false)
    private String firstName;
    @Column(name = "last_name", nullable = false)
    private String lastName;
    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false,columnDefinition = "SMALLINT")
    @Positive(message = "The height is invalid")
    private short height; // en cm

    @Column(nullable = false, columnDefinition = "SMALLINT")
    @Positive(message = "The weight is invalid")
    private short weight; // en kg

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_level")
    private ActivityLevel activityLevel;

    @Enumerated(EnumType.STRING)
    private Goal goal;

    @ElementCollection
    @CollectionTable(
            name = "user_allergies",    // le nom de la table
            joinColumns = @JoinColumn(name = "user_id") // le nom de la colonne en ref
    )
    @Column(name = "allergy")
    private List<String> allergies;


    @ElementCollection
    @CollectionTable(
            name = "user_preferences",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "preference")
    private List<String> dietaryPreference;
}
