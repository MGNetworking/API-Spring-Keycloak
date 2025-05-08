package com.nutrition.API_nutrition.service;

import com.nutrition.API_nutrition.exception.ApiException;
import com.nutrition.API_nutrition.exception.ErrorCode;
import com.nutrition.API_nutrition.model.dto.RegisterRequestDto;
import com.nutrition.API_nutrition.model.dto.UserResponseDto;
import com.nutrition.API_nutrition.model.entity.User;
import com.nutrition.API_nutrition.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.HttpStatus;
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
    public UserResponseDto createUser(RegisterRequestDto dtoSave) {

        if (dtoSave == null) {
            log.info("This dto for create user is null");

            throw new ApiException(
                    "This dto for create user is null",
                    HttpStatus.NOT_FOUND,
                    ErrorCode.DB_ERROR.toString()
            );
        }

        try {

            // Étape 1 - Créer l'utilisateur dans Keycloak
            this.keycloakService.createUser(dtoSave);

            try {
                // Étape 2 - Ajouter les rôles
                //keycloakService.addUserRolesRealm(dtoSave.getKeycloakId(), List.of("USER"));
                keycloakService.addUserRolesClient(dtoSave.getKeycloakId(), List.of("USER"));

            } catch (Exception e) {
                // Supprimer dans Keycloak si l'ajout de rôle échoue
                log.warn("Role assignment failed, user Keycloak deleted: {}", e.getMessage(), e);
                boolean status = keycloakService.removeUser(dtoSave.getKeycloakId());

                if (status) {
                    throw new ApiException(
                            "Failed to assign roles",
                            HttpStatus.BAD_REQUEST,
                            ErrorCode.USER_ROLE_ASSIGNMENT_FAILED.toString()
                    );
                } else {
                    throw new ApiException(
                            "Failed to assign roles and rollback in keycloak is was unsuccessful",
                            HttpStatus.BAD_REQUEST,
                            ErrorCode.USER_ROLE_ASSIGNMENT_FAILED.toString()
                    );
                }

            }

            User updateUser = this.userRepository.save(dtoSave.UserMapping());
            userRepository.flush();
            return new UserResponseDto().mappingToUser(updateUser);

        } catch (ApiException e) {
            // Si une ApiException est déjà lancée, la capturer sans la modifier
            log.error("API error when create user: {}", e.getMessage(), e);
            throw e;  // Relance de l'exception sans modification

        } catch (DataIntegrityViolationException e) {
            // Gère les violations de contraintes (clés uniques, NOT NULL, etc.)
            log.error("Database constraint violation when saving user: {}", e.getMessage(), e);

            throw new ApiException(
                    "Could not save user",
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.DB_ERROR.toString()
            );

        } catch (JpaSystemException | TransientDataAccessException e) {
            // Gère les erreurs de validation JPA ou les problèmes de transaction
            log.error("JPA or transaction error when saving user: {}", e.getMessage(), e);

            throw new ApiException(
                    "Error during user creation",
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.DB_ERROR.toString()
            );

        } catch (DataAccessException e) {
            // Erreur liée à Spring Data / la base
            log.warn("User persistence failed, Keycloak deleted.");

            if (dtoSave.getKeycloakId() != null) {
                keycloakService.removeUser(dtoSave.getKeycloakId());
                log.info("Keycloak user removed during cleanup.");
            } else {
                log.error("Failed to clean up Keycloak user: {}", e.getMessage(), e);
            }

            throw new ApiException(
                    "Persistence failure to user",
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.DB_ERROR.toString()
            );

        } catch (Exception e) {
            // Attrape toutes les autres exceptions non prévues
            log.error("Unexpected error when saving user: {}", e.getMessage(), e);
            throw new ApiException(
                    "Failed to create user",
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.USER_CREATION_FAILED.toString()
            );

        }

    }

    public Optional<User> getuser(String userId) throws IllegalArgumentException {
        try {
            return this.userRepository.findByKeycloakId(userId);

        } catch (Exception e) {
            log.error("Error searching for user with ID {}", userId);
            throw new ApiException(
                    "Failed to research user",
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.USER_RESEARCH_FAILED.toString()
            );
        }

    }

    @Transactional
    public UserResponseDto updateUser(RegisterRequestDto dto) {

        if (dto == null || dto.getKeycloakId().isEmpty()) {
            log.info("This dto for update User is null");
            throw new ApiException("This user cannot be null or user ID empty",
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.USER_RESEARCH_FAILED.toString());
        }

        try {

            // Met à jour l'utilisateur dans Keycloak
            boolean keycloakUpdateSuccess = this.keycloakService.updateUser(dto);
            if (!keycloakUpdateSuccess) {
                throw new ApiException(
                        "Failed to update user in Keycloak",
                        HttpStatus.BAD_REQUEST,
                        ErrorCode.USER_UPDATE_FAILED.toString()
                );
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


        } catch (ApiException e) {
            // Si une ApiException est déjà lancée, la capturer sans la modifier
            log.error("API error when updating user: {}", e.getMessage(), e);
            throw e;  // Relance de l'exception sans modification

        } catch (DataIntegrityViolationException e) {
            // Gère les violations de contraintes (clés uniques, NOT NULL, etc.)
            log.error("Database constraint violation when updating user: {}", e.getMessage(), e);
            throw new ApiException(
                    "Could not update user due to data constraints",
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.DB_ERROR.toString()
            );

        } catch (JpaSystemException | TransientDataAccessException e) {
            // Gère les erreurs de validation JPA ou les problèmes de transaction
            log.error("JPA or transaction error when updating user: {}", e.getMessage(), e);
            throw new ApiException(
                    "Error during user update transaction",
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.DB_ERROR.toString()
            );

        } catch (Exception e) {
            // Attrape toutes les autres exceptions non prévues
            log.error("Unexpected error when updating user: {}", e.getMessage(), e);
            throw new ApiException(
                    "Failed to update user",
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.TECHNICAL_ERROR.toString()
            );
        }

    }

    @Transactional
    public void deleteUser(String keycloakId) {

        if (keycloakId == null || keycloakId.isEmpty()) {
            log.info("KeycloakId for user deletion is null or empty");
            throw new ApiException(
                    "KeycloakId cannot be null or empty",
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.BAD_REQUEST_PARAMETER.toString()
            );
        }

        try {

            // Supprimer dans Keycloak
            boolean keycloakUpdateSuccess = this.keycloakService.removeUser(keycloakId);
            if (!keycloakUpdateSuccess) {
                throw new ApiException(
                        "Failed to delete user in Keycloak",
                        HttpStatus.BAD_REQUEST,
                        ErrorCode.KEYCLOAK_BAD_REQUEST.toString()
                );
            }

            // Supprimer en base de données
            this.userRepository.deleteById(keycloakId);

            // check si l'utilisateur existe en BD
            boolean stillExists = userRepository.existsById(keycloakId);
            if (stillExists) {
                throw new ApiException(
                        "User was not deleted from database",
                        HttpStatus.BAD_REQUEST,
                        ErrorCode.DB_ERROR.toString()
                );
            }

            log.info("User with keycloakId {} successfully deleted", keycloakId);

        } catch (ApiException e) {
            // Si une ApiException est déjà lancée, la capturer sans la modifier
            log.error("API error when delete user: {}", e.getMessage(), e);
            throw e;  // Relance de l'exception sans modification

        } catch (DataIntegrityViolationException e) {
            // Gère les violations de contraintes (clés uniques, NOT NULL, etc.)
            log.error("Database constraint violation when deleting user: {}", e.getMessage(), e);
            throw new ApiException(
                    "Could not delete user due to data constraints",
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.DB_ERROR.toString()
            );

        } catch (JpaSystemException | TransientDataAccessException e) {
            // Gère les erreurs de validation JPA ou les problèmes de transaction
            log.error("JPA or transaction error when deleting user: {}", e.getMessage(), e);
            throw new ApiException(
                    "Error during user deletion transaction",
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.DB_ERROR.toString()
            );

        } catch (Exception e) {
            // Attrape toutes les autres exceptions non prévues
            log.error("Unexpected error when deleting user: {}", e.getMessage(), e);
            throw new ApiException(
                    "Failed to delete user",
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.TECHNICAL_ERROR.toString()
            );
        }

    }

}
