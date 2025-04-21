package com.nutrition.API_nutrition.model.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class GenericApiResponse<D> {

    private String timestamp;
    private HttpStatus status;
    private String message;
    private String path;
    private D data;

    public GenericApiResponse(HttpStatus status, String message, String path, D data) {
        this.timestamp = LocalDate.now().toString();
        this.status = status;
        this.message = message;
        this.path = path;
        this.data = data;
    }

}
