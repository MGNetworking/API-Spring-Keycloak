package com.nutrition.API_nutrition.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "meal_items")
public class MealItems {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "meal_id")
    private Meal meal;

    @Column(name = "food_external_id")
    private String foodExternalId;   // API externe

    @Column(name = "food_name")
    private String foodName;

    private Float quantity; // en gramme
    private Float calories;
    private Float proteins;
    private Float carbs;
    private Float fats;

}
