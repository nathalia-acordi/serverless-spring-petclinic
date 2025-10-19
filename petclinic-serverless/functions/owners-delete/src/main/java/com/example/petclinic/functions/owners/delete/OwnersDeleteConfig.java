package com.example.petclinic.functions.owners.delete;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.function.Function;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.example.petclinic.domain.owner.OwnerRepository;
import com.example.petclinic.domain.owner.OwnerService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

@SpringBootApplication(scanBasePackages = "com.example.petclinic")
public class OwnersDeleteConfig {

    @Bean("ownersDelete")
    public Function<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> ownersDelete(DeleteOwnerFunction handler) {
        return handler;
    }

    @Bean
    public OwnerService ownerService(OwnerRepository ownerRepository) {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        return new OwnerService(ownerRepository, validator);
    }
}
