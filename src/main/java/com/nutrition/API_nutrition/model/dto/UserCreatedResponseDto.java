package com.nutrition.API_nutrition.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeycloakLoginRequestDto {

    @Schema(description = "Username (avatar name)", required = true)
    @NotBlank(message = "Username is required")
    private String userName;

    @Schema(description = "Password", required = true)
    @NotBlank(message = "Password is required")
    private String password;

}