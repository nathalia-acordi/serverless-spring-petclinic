package com.example.petclinic.functions.owners.create;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.function.Function;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;

/**
 * Configuration class exposing the ownersCreate function bean so that
 * Spring Cloud Function's FunctionInvoker can locate it using the
 * SPRING_CLOUD_FUNCTION_DEFINITION environment variable.
 */
@SpringBootApplication(scanBasePackages = "com.example.petclinic")
public class OwnersCreateConfig {

    @Bean("ownersCreate")
    public Function<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> ownersCreate(CreateOwnerFunction handler) {
        return handler;
    }
}
