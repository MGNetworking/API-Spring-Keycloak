package com.nutrition.API_nutrition.repository;

import com.nutrition.API_nutrition.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {


}
