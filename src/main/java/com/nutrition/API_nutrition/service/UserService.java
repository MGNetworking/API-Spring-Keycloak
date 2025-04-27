package com.nutrition.API_nutrition.service;

import com.nutrition.API_nutrition.model.dto.RegisterRequestDto;
import com.nutrition.API_nutrition.model.dto.UserResponseDto;
import com.nutrition.API_nutrition.model.entity.User;
import com.nutrition.API_nutrition.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.service.spi.ServiceException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final KeycloakService keycloakService;

    /**
     * Permet d'enregistrer un utilisateur en DB
     *
     * @param dtoSave RegisterRequestDto
     * @return UserResponseDto
     */
    @Transactional
    public UserResponseDto createUser(RegisterRequestDto dtoSave) throws IllegalArgumentException {

        if (dtoSave == null) {
            log.info("This dto for create user is null");
            throw new IllegalArgumentException("This user dto is null");
        }

        try {

            // Créer l'utilisateur dans Keycloak
            this.keycloakService.createUser(dtoSave);

            // Attribuer le rôle de base "USER" à tous les nouveaux utilisateurs
            keycloakService.addUserRoles(dtoSave.getKeycloakId(), List.of("USER"));

            try {

                User updateUser = this.userRepository.save(dtoSave.UserMapping());
                userRepository.flush();
                return new UserResponseDto().mappingToUser(updateUser);
            } catch (Exception e) {
                // En cas d'échec de la base de données, annuler l'opération Keycloak
                this.keycloakService.removeUser(dtoSave.getKeycloakId());
                throw e;
            }

        } catch (DataIntegrityViolationException e) {
            // Gère les violations de contraintes (clés uniques, NOT NULL, etc.)
            log.error("Database constraint violation when saving user: {}", e.getMessage());
            throw new IllegalArgumentException("Could not save user: " + e.getMessage(), e);

        } catch (JpaSystemException | TransientDataAccessException e) {
            // Gère les erreurs de validation JPA ou les problèmes de transaction
            log.error("JPA or transaction error when saving user: {}", e.getMessage());
            throw new IllegalArgumentException("Error during user creation: " + e.getMessage(), e);

        } catch (Exception e) {
            // Attrape toutes les autres exceptions non prévues
            log.error("Unexpected error when saving user", e);
            throw new IllegalArgumentException("Failed to create user: " + e.getMessage(), e);
        }

    }

    public Optional<User> getuser(String userId) throws IllegalArgumentException {
        try {
            return this.userRepository.findByKeycloakId(userId);

        } catch (Exception e) {
            log.error("Error searching for user with ID {}", userId);
            throw new IllegalArgumentException("Failed to research user");
        }

    }

    @Transactional
    public UserResponseDto updateUser(RegisterRequestDto dto) {

        if (dto == null || dto.getKeycloakId().isEmpty()) {
            log.info("This dto for update User is null");
            throw new IllegalArgumentException("This user cannot be null or user ID empty");
        }

        try {

            // Met à jour l'utilisateur dans Keycloak
            boolean keycloakUpdateSuccess = this.keycloakService.updateUser(dto);
            if (!keycloakUpdateSuccess) {
                throw new ServiceException("Failed to update user in Keycloak");
            }
            try {

                User updateUser = this.userRepository.save(dto.UserMapping());
                userRepository.flush();
                return new UserResponseDto().mappingToUser(updateUser);
            } catch (Exception e) {
                // En cas d'échec de la base de données, annuler l'opération Keycloak
                this.keycloakService.removeUser(dto.getKeycloakId());
                throw e;
            }


        } catch (DataIntegrityViolationException e) {
            // Gère les violations de contraintes (clés uniques, NOT NULL, etc.)
            log.error("Database constraint violation when updating user:{}", e.getMessage());
            throw new IllegalArgumentException("Could not update user due to data constraints", e);

        } catch (JpaSystemException | TransientDataAccessException e) {
            // Gère les erreurs de validation JPA ou les problèmes de transaction
            log.error("JPA or transaction error when updating user: {}", e.getMessage());
            throw new IllegalArgumentException("Error during user update transaction ", e);

        } catch (Exception e) {
            // Attrape toutes les autres exceptions non prévues
            log.error("Unexpected error when updating user", e);
            throw new IllegalArgumentException("Failed to update user", e);
        }

    }

    @Transactional
    public void deleteUser(String keycloakId) {

        if (keycloakId == null || keycloakId.isEmpty()) {
            log.info("KeycloakId for user deletion is null or empty");
            throw new IllegalArgumentException("KeycloakId cannot be null or empty");
        }

        try {

            // Supprimer dans Keycloak
            boolean keycloakUpdateSuccess = this.keycloakService.removeUser(keycloakId);
            if (!keycloakUpdateSuccess) {
                throw new IllegalArgumentException("Failed to delete user in Keycloak");
            }

            // Supprimer en base de données
            this.userRepository.deleteById(keycloakId);

            // check si l'utilisateur existe en BD
            boolean stillExists = userRepository.existsById(keycloakId);
            if (stillExists) {
                throw new IllegalArgumentException("User was not deleted from database");
            }

            log.info("User with keycloakId {} successfully deleted", keycloakId);

        } catch (DataIntegrityViolationException e) {
            // Gère les violations de contraintes (clés uniques, NOT NULL, etc.)
            log.error("Database constraint violation when deleting user: {}", e.getMessage());
            throw new IllegalArgumentException("Could not delete user due to data constraints", e);

        } catch (JpaSystemException | TransientDataAccessException e) {
            // Gère les erreurs de validation JPA ou les problèmes de transaction
            log.error("JPA or transaction error when deleting user: {}", e.getMessage());
            throw new IllegalArgumentException("Error during user deletion transaction", e);

        } catch (Exception e) {
            // Attrape toutes les autres exceptions non prévues
            log.error("Unexpected error when deleting user", e);
            throw new IllegalArgumentException("Failed to delete user" , e);
        }

    }

}
