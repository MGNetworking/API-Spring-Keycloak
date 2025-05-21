package com.nutrition.API_nutrition.model.dto;

import com.nutrition.API_nutrition.model.validation.OnCreateOrUpdateAdmin;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleUser {

    @Schema(description = "Flag indicating whether roles should be updated", example = "true")
    @NotNull(message = "Update flag is required", groups = OnCreateOrUpdateAdmin.class)
    private boolean update;

    @Schema(description = "List of role names to assign to the user", example = "[\"admin\", \"user\"]")
    @NotNull(message = "Roles list cannot be null", groups = OnCreateOrUpdateAdmin.class)
    @Size(min = 1, message = "At least one role must be provided if update is true", groups = OnCreateOrUpdateAdmin.class)
    private List<String> roles;
}
