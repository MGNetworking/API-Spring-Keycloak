package com.nutrition.API_nutrition.service;

import com.nutrition.API_nutrition.model.dto.UserDtoSave;
import com.nutrition.API_nutrition.model.entity.User;
import com.nutrition.API_nutrition.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(UserDtoSave dtoSave) throws IllegalArgumentException {

        if (dtoSave == null) {
            log.info("This dto for create user is null");
            throw new IllegalArgumentException("This user dto is null");
        }

        try{
            return this.userRepository.save(dtoSave.mapping());
        } catch (DataIntegrityViolationException e){
            // Gère les violations de contraintes (clés uniques, NOT NULL, etc.)
            log.error("Database constraint violation when saving user: {}", e.getMessage());
            throw new IllegalArgumentException("Could not save user: " + e.getMessage(), e);

        } catch (JpaSystemException | TransientDataAccessException e){
            // Gère les erreurs de validation JPA ou les problèmes de transaction
            log.error("JPA or transaction error when saving user: {}", e.getMessage());
            throw new IllegalArgumentException("Error during user creation: " + e.getMessage(), e);

        } catch (Exception e){
            // Attrape toutes les autres exceptions non prévues
            log.error("Unexpected error when saving user", e);
            throw new IllegalArgumentException("Failed to create user: " + e.getMessage(), e);
        }

    }
}
