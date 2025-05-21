package com.nutrition.API_nutrition.model.dto;

import com.nutrition.API_nutrition.model.validation.OnAdminUpdate;
import com.nutrition.API_nutrition.model.validation.OnCreateOrUpdateAdmin;
import com.nutrition.API_nutrition.model.validation.OnCreateOrUpdateUser;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeycloakUserData {

    @Schema(description = "Keycloak user identifier", example = "123e4567-e89b-12d3-a456-426614174000", required = false)
    private String keycloakId;

    @Schema(description = "Username (avatar name)", example = "johndoe", required = true)
    @NotBlank(message = "Username is required", groups = OnCreateOrUpdateUser.class)
    private String userName;

    @Schema(description = "First name", example = "John")
    @NotBlank(message = "First name is required", groups = OnCreateOrUpdateUser.class)
    private String firstName;

    @Schema(description = "Last name", example = "Doe")
    @NotBlank(message = "Last name is required", groups = OnCreateOrUpdateUser.class)
    private String lastName;

    @Schema(description = "Password", example = "P@ssw0rd123")
    @NotBlank(message = "Password is required", groups = OnCreateOrUpdateUser.class)
    private String password;

    @Schema(description = "Email address", example = "johndoe.dupont@gmail.com")
    @Email(message = "Invalid email address")
    @NotBlank(message = "Email is required", groups = OnCreateOrUpdateUser.class)
    private String email;

    @Schema(description = "Indicates whether the user's email address has been verified (validated by the user)")
    @NotNull(message = "Verification of the user's email address is required", groups = OnCreateOrUpdateAdmin.class)
    private boolean emailVerified;

    @Schema(description = "Indicates whether the user account is enabled or disabled.")
    @NotNull(message = "Indication of user account status is not required", groups = OnCreateOrUpdateAdmin.class)
    private boolean enabled;

    @Valid
    @Schema(description = "Defines whether the user's realm roles should be updated, and provides the list of roles.")
    @NotNull(message = "Realm roles update information is required", groups = OnCreateOrUpdateAdmin.class)
    private RoleUser rolesRealm;

    @Valid
    @Schema(description = "Defines whether the user's client roles should be updated, and provides the list of roles.")
    @NotNull(message = "Client roles update information is required", groups = OnCreateOrUpdateAdmin.class)
    private RoleUser rolesClient;
}
