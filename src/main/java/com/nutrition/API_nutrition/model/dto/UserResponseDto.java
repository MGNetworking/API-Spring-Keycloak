package com.nutrition.API_nutrition.model.dto;

import com.nutrition.API_nutrition.model.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Classe d'identité ou d'information d'un utilisateur
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponseDto implements ApiResponseData {

    private String keycloakId;          // ID de keycloak
    private String userName;            // Nom d'utilisateur
    private String firstName;           // Prénom
    private String lastName;            // Nom de famille
    private String email;               // Email de l'utilisateur
    private LocalDateTime createdAt;    // Date de création du compte avec l'heure minute second
    private LocalDateTime updatedAt;    // Date de la mise à jour

    /**
     * Gere le mapping de l'objet User vers le DTO de cette classe
     *
     * @param user entity
     * @return UserResponseDto
     */
    public UserResponseDto mappingToUser(User user) {
        UserResponseDto dto = new UserResponseDto();
        dto.setKeycloakId(user.getKeycloakId());
        dto.setUserName(user.getUsername());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());

        return dto;
    }
}
