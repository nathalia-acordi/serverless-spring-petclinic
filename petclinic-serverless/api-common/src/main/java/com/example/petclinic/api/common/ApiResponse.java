package com.example.petclinic.api.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Value;

/**
 * Generic envelope for HTTP style responses inside Lambda functions.
 * statusCode is used by the adapter mapping layer to decide HTTP code.
 */
@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    int statusCode;
    T body;
    ErrorResponse error;
}
