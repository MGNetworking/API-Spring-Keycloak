package com.nutrition.API_nutrition.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "diets")
public class Diets {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private DietType dietType;

    @Column(name = "calorie_target")
    private Float calorieTarget;

    // Sous element dans la table Diets
    @Embedded
    private MacroDistribution macroDistribution;
}
