package com.nutrition.API_nutrition.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenResponseDto implements ApiResponseData {

    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    private long refreshExpiresIn;

}
