package com.nutrition.API_nutrition.model.dto;

import com.nutrition.API_nutrition.model.entity.ActivityLevel;
import com.nutrition.API_nutrition.model.entity.Gender;
import com.nutrition.API_nutrition.model.entity.Goal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserOutputDto implements ApiResponseData {

    public String keycloakId;
    public LocalDate birthdate;
    private Gender gender;
    public short height;
    public short weight;
    private ActivityLevel activityLevel;
    private Goal goal;
    private List<String> allergies;
    private List<String> dietaryPreference;
}
