package com.nutrition.API_nutrition.service;

import com.nutrition.API_nutrition.config.UserFactory;
import com.nutrition.API_nutrition.exception.ApiException;
import com.nutrition.API_nutrition.exception.ErrorCode;
import com.nutrition.API_nutrition.model.dto.UserInputDTO;
import com.nutrition.API_nutrition.model.dto.UserOutputDto;
import com.nutrition.API_nutrition.model.entity.User;
import com.nutrition.API_nutrition.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserFactory userFactory;

    /**
     * Permet d'enregistrer un utilisateur en DB
     */
    public void createUser(String userId) {

        if (userId == null || userId.isEmpty()) {
            log.error("This dto for create user is null");

            throw new ApiException(
                    "This dto for create user is null",
                    HttpStatus.NOT_FOUND,
                    ErrorCode.INVALID_USER_ID.toString()
            );
        }

        try {
            User user = new User();
            user.setKeycloakId(userId);

            this.userRepository.save(user);
            log.info("User with ID {} successfully inserted into database.", userId);

        } catch (DataIntegrityViolationException e) {
            // Erreur de contrainte en base (clé primaire, unique, etc.)
            log.error("Database constraint violation while inserting userId {}: {}", userId, e.getMessage(), e);
            throw new ApiException(
                    "A user with this ID already exists or violates constraints.",
                    HttpStatus.CONFLICT,
                    ErrorCode.DB_CONSTRAINT_VIOLATION.toString()
            );

        } catch (JpaSystemException | TransientDataAccessException e) {
            // Problèmes liés à JPA ou aux transactions
            log.error("JPA or transaction error during user creation for ID {}: {}", userId, e.getMessage(), e);
            throw new ApiException(
                    "Error during user creation",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorCode.DB_SYSTEM_ERROR.toString()
            );

        } catch (DataAccessException e) {
            // Autres erreurs Spring Data (connexion, SQL, etc.)
            log.error("DataAccessException during user creation for ID {}: {}", userId, e.getMessage(), e);
            throw new ApiException(
                    "Database access error",
                    HttpStatus.SERVICE_UNAVAILABLE,
                    ErrorCode.DB_ERROR.toString()
            );

        } catch (Exception e) {
            // Toute autre exception inattendue
            log.error("Unexpected error during user creation for ID {}: {}", userId, e.getMessage(), e);
            throw new ApiException(
                    "Unexpected error occurred",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorCode.TECHNICAL_ERROR.toString()
            );
        }
    }


    public UserOutputDto getUser(String userId) {
        try {
            return this.userRepository.findById(userId)
                    .map(userFactory::userToUserDtoOutput)
                    .orElseThrow(() -> new ApiException(
                            "User not found with id: " + userId,
                            HttpStatus.NOT_FOUND,
                            ErrorCode.DB_USER_NOT_FOUND.toString()
                    ));

        } catch (ApiException e) {
            //  on laisse remonter.
            throw e;

        } catch (DataIntegrityViolationException e) {
            log.error("Constraint violation while searching for user with ID {}", userId, e);
            throw new ApiException(
                    "Database constraint violation",
                    HttpStatus.CONFLICT,
                    ErrorCode.DB_CONSTRAINT_VIOLATION.toString()
            );

        } catch (JpaSystemException | TransientDataAccessException e) {
            log.error("JPA system or transient error for user ID {}", userId, e);
            throw new ApiException(
                    "Database system error",
                    HttpStatus.SERVICE_UNAVAILABLE,
                    ErrorCode.DB_SYSTEM_ERROR.toString()
            );

        } catch (DataAccessException e) {
            log.error("Generic data access error for user ID {}", userId, e);
            throw new ApiException(
                    "Database access error",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorCode.DB_ERROR.toString()
            );

        } catch (Exception e) {
            log.error("Unexpected error while searching for user with ID {}", userId, e);
            throw new ApiException(
                    "Unexpected error occurred",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorCode.TECHNICAL_ERROR.toString()
            );
        }
    }


    @Transactional
    public User updateUser(UserInputDTO userInputDTO) {

        if (userInputDTO == null || userInputDTO.getKeycloakId() == null || userInputDTO.getKeycloakId().isBlank()) {
            log.warn("Invalid DTO for user update: dto or keycloakId is null/blank");
            throw new ApiException(
                    "User data is missing or invalid",
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.INVALID_USER_DATA.toString());
        }

        try {

            User existingUser = userRepository.findById(userInputDTO.getKeycloakId())
                    .orElseThrow(() -> {
                        log.info("User not found for update: {}", userInputDTO.getKeycloakId());
                        return new ApiException(
                                "User not found, update is impossible",
                                HttpStatus.NOT_FOUND,
                                ErrorCode.DB_USER_NOT_FOUND.toString());
                    });

            userFactory.updateUserFromDto(existingUser,userInputDTO);
            User updateUser = this.userRepository.save(existingUser);
            this.userRepository.flush();

            log.info("User successfully updated: {}", updateUser.getKeycloakId());
            return updateUser;


        } catch (ApiException e) {
            //  on laisse remonter.
            throw e;
        } catch (DataIntegrityViolationException e) {
            // Violations de contraintes d'intégrité (clé unique, NOT NULL, nullable etc.)
            log.error("Integrity violation during user update: {}", e.getMessage(), e);
            throw new ApiException(
                    "The update failed: some of the data did not comply with the expected constraints",
                    HttpStatus.CONFLICT,
                    ErrorCode.DB_CONSTRAINT_VIOLATION.toString()
            );

        } catch (JpaSystemException | TransientDataAccessException e) {
            // Erreurs JPA ou transactionnelles (surcharge temporaire, connexion perdue)
            log.error("JPA or transactional error during update: {}", e.getMessage(), e);
            throw new ApiException(
                    "A technical error has occurred during the update. Please try again later.",
                    HttpStatus.SERVICE_UNAVAILABLE,
                    ErrorCode.DB_ERROR.toString()
            );

        } catch (DataAccessException e) {
            // Problème global d'accès à la base de données
            log.error("Database access failed during user update: {}", e.getMessage(), e);
            throw new ApiException(
                    "Unable to update user data due to database access problem.",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorCode.DB_ERROR.toString()
            );

        } catch (Exception e) {
            // Erreur inconnue, attrapée en dernier recours
            log.error("Unexpected error during user update: {}", e.getMessage(), e);
            throw new ApiException(
                    "An unexpected error has occurred during the update. Please contact support.",
                    HttpStatus.INTERNAL_SERVER_ERROR,
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

            // Suppression directe
            userRepository.deleteById(keycloakId);
            userRepository.flush();

            log.info("User with keycloakId {} successfully deleted", keycloakId);

        } catch (EmptyResultDataAccessException e) {
            // Tentative de suppression d'un utilisateur inexistant
            log.warn("Attempted to delete non-existing user with keycloakId {}", keycloakId);
            throw new ApiException(
                    "User not found for deletion",
                    HttpStatus.NOT_FOUND,
                    ErrorCode.DB_USER_NOT_FOUND.toString()
            );

        } catch (DataIntegrityViolationException e) {
            // Violation de contraintes (ex: clé étrangère empêchant suppression)
            log.error("Database constraint violation when deleting user: {}", e.getMessage(), e);
            throw new ApiException(
                    "Cannot delete user due to database constraints",
                    HttpStatus.CONFLICT,
                    ErrorCode.DB_CONSTRAINT_VIOLATION.toString()
            );

        } catch (JpaSystemException | TransientDataAccessException e) {
            // Erreurs JPA ou transactionnelles
            log.error("JPA or transaction error during user deletion: {}", e.getMessage(), e);
            throw new ApiException(
                    "Error during user deletion transaction",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorCode.DB_ERROR.toString()
            );

        } catch (Exception e) {
            // Erreur inattendue
            log.error("Unexpected error when deleting user: {}", e.getMessage(), e);
            throw new ApiException(
                    "Failed to delete user due to unexpected error",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorCode.TECHNICAL_ERROR.toString()
            );
        }

    }

}
