package com.nutrition.API_nutrition.model.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
@JsonPropertyOrder({
        "timestamp",
        "status",
        "statusCode",
        "message",
        "path",
        "data"
})
@Data
@NoArgsConstructor
public class GenericApiResponse<D> {

    private LocalDateTime timestamp;
    private HttpStatus status;
    private int statusCode;
    private String message;
    private String path;
    private D data;

    public GenericApiResponse(HttpStatus status, int statusCode, String message, String path, D data) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.statusCode = statusCode;
        this.message = message;
        this.path = path;
        this.data = data;
    }

}
