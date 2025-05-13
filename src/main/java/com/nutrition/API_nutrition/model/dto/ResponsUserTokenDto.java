package com.nutrition.API_nutrition.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponsUserTokenDto implements ApiResponseData {

    private UserResponseDto userResponseDto;
    private TokenResponseDto token;
}
