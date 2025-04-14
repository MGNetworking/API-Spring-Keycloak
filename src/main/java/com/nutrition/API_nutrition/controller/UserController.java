package com.nutrition.API_nutrition.controller;

import com.nutrition.API_nutrition.model.dto.UserDtoSave;
import com.nutrition.API_nutrition.model.entity.User;
import com.nutrition.API_nutrition.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1")
public class UserController {

    private UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping(value = "/auth/register")
    public User postUser(@Valid @RequestBody UserDtoSave userDto) {

        log.info("User DTO {}", userDto);
        return this.userService.createUser(userDto);
    }
}
