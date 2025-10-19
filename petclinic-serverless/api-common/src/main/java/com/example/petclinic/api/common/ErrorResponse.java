package com.example.petclinic.api.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Value;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    String code;
    String message;
}
