package com.navan.billing.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    String error;
    String message;
    List<String> details;
    Long existingId;

    public static ApiError of(String error, String message) {
        return ApiError.builder().error(error).message(message).build();
    }
}
