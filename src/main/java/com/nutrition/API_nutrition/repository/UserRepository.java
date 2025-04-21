package com.nutrition.API_nutrition.repository;

import com.nutrition.API_nutrition.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByKeycloakId(String keycloakId);

    Optional<User> findByFirstNameAndLastNameAndEmail(String firstName, String lastName, String email);
}
