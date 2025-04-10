package com.nutrition.API_nutrition.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "meals")
public class Meal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    private String name;

    @OneToMany(mappedBy = "meal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MealItems> mealItems;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type")
    private MealType type;

    @Column(name = "date_time")
    private LocalDateTime date;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
