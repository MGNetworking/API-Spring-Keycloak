package com.nutrition.API_nutrition.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Fait partie de Diets
 */
@Embeddable
public class MacroDistribution {

    @Column(name = "protein_percentage")
    private Float proteinPercentage;

    @Column(name = "carb_percentage")
    private Float carbPercentage;

    @Column(name = "fat_percentage")
    private Float fatPercentage;
}
