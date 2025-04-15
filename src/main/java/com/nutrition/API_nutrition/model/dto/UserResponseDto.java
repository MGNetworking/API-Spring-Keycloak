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
public class UserResponseDto {

    private Long id;                  // ID interne de l'utilisateur dans votre BDD
    private String username;            // Nom d'utilisateur
    private String firstName;           // Prénom
    private String lastName;            // Nom de famille
    private String email;               // Email de l'utilisateur
    private LocalDateTime createdAt;    // Date de création du compte avec l'heure minute second

    /**
     * Gere le mapping de l'objet User vers le DTO de cette classe
     *
     * @param user entity
     * @return UserResponseDto
     */
    public UserResponseDto mappingToUser(User user) {
        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setCreatedAt(user.getCreatedAt());

        return dto;
    }
}
