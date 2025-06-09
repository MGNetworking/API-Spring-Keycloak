package com.nutrition.API_nutrition.config;

import com.nutrition.API_nutrition.model.dto.UserInputDTO;
import com.nutrition.API_nutrition.model.dto.UserOutputDto;
import com.nutrition.API_nutrition.model.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserFactory {

    /**
     * Mapping d'un {@link UserInputDTO} vers un {@link User}
     *
     * @param dto {@link UserInputDTO}
     * @return {@link User}
     */
    public User userDtoInputToUser(UserInputDTO dto) {
        User user = new User();
        user.setKeycloakId(dto.getKeycloakId());
        user.setGender(dto.getGender());
        user.setHeight(dto.getHeight());
        user.setWeight(dto.getWeight());
        user.setBirthDate(dto.getBirthdate());
        return user;
    }

    /**
     * Mapping d'un {@link User} vers un {@link UserOutputDto }
     *
     * @param user {@link User}
     * @return {@link UserOutputDto}
     */
    public UserOutputDto userToUserDtoOutput(User user) {
        UserOutputDto dtoComplet = new UserOutputDto();
        dtoComplet.setKeycloakId(user.getKeycloakId());
        dtoComplet.setBirthdate(user.getBirthDate());
        dtoComplet.setGender(user.getGender());
        dtoComplet.setHeight(user.getHeight());
        dtoComplet.setWeight(user.getWeight());
        dtoComplet.setActivityLevel(user.getActivityLevel());
        dtoComplet.setGoal(user.getGoal());
        dtoComplet.setAllergies(user.getAllergies());
        dtoComplet.setDietaryPreference(user.getDietaryPreference());

        return dtoComplet;
    }

    /**
     * Modification d'un {@link User} via {@link UserInputDTO }
     *
     * @param user {@link User} et {@link UserInputDTO }
     */
    public void updateUserFromDto(User user, UserInputDTO inputDTO) {
        user.setKeycloakId(inputDTO.getKeycloakId());
        user.setBirthDate(inputDTO.getBirthdate());
        user.setGender(inputDTO.getGender());
        user.setHeight(inputDTO.getHeight());
        user.setWeight(inputDTO.getWeight());
    }
}
