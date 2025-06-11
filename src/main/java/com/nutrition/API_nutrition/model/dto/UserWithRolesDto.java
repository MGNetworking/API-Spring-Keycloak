package com.nutrition.API_nutrition.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserWithRolesDto {

    private UserRepresentation userRep;
    private List<RoleRepresentation> lsRole;
}
