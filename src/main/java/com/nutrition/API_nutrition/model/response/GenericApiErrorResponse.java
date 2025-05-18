package com.nutrition.API_nutrition.model.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({
        "timestamp",
        "status",
        "statusCode",
        "message",
        "path",
        "errorCode",
        "data"
})
@Data
@NoArgsConstructor
public class GenericApiErrorResponse<D> extends GenericApiResponse<D> {

    private String errorCode;

    public GenericApiErrorResponse(HttpStatus status,
                                   int statusCode,
                                   String message,
                                   String path,
                                   D data,
                                   String errorCode) {
        super(status, statusCode, message, path, data);
        this.errorCode = errorCode;
    }
}
