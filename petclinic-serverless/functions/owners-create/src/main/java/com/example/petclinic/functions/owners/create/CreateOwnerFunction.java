package com.example.petclinic.functions.owners.create;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.example.petclinic.domain.owner.Owner;
import com.example.petclinic.domain.owner.OwnerService;
import com.example.petclinic.domain.owner.OwnerValidationException;
import com.example.petclinic.api.common.http.ApiResponses;
import com.example.petclinic.api.common.metrics.MetricsSupport;
import com.example.petclinic.api.common.validation.ValidationSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import software.amazon.lambda.powertools.logging.Logging; 
import software.amazon.lambda.powertools.tracing.Tracing;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.function.Function;

/**
 * Spring Cloud Function bean mapping POST /owners to creation logic.
 * Powertools annotations provide structured logs, metrics and traces.
 */
@Slf4j
@Component
public class CreateOwnerFunction implements Function<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private final OwnerService ownerService;
    private final ObjectMapper mapper = new ObjectMapper();

    public CreateOwnerFunction(OwnerService ownerService) {
        this.ownerService = ownerService;
    }

    @Override
    @Logging(logEvent = true) // log request/response as structured JSON
    @Tracing(namespace = "Petclinic")
    public APIGatewayV2HTTPResponse apply(APIGatewayV2HTTPEvent event) {
        long handlerStart = MetricsSupport.startTimer();
        try {
            long serviceStart = MetricsSupport.startTimer();
            try {
                if (event == null || event.getBody() == null || event.getBody().isBlank()) {
                    return ApiResponses.badRequest("BAD_REQUEST", "Empty body");
                }
                CreateOwnerRequest request = mapper.readValue(event.getBody(), CreateOwnerRequest.class);
                ValidationSupport.validate(request);
                Owner toCreate = Owner.builder()
                        .firstName(request.firstName())
                        .lastName(request.lastName())
                        .address(request.address())
                        .city(request.city())
                        .telephone(request.telephone())
                        .build();
                Owner saved = ownerService.create(toCreate);
                MetricsSupport.increment("OwnersCreatedCount", "Owners", "POST_/owners");
                MetricsSupport.publishTimer("OwnersServiceCreateLatencyMs", MetricsSupport.endTimer(serviceStart), "Owners", "POST_/owners");
                log.info("[OwnersCreate] Owner created id={} firstName={} lastName={}", saved.getId(), saved.getFirstName(), saved.getLastName());
                return ApiResponses.created(OwnerDto.from(saved));
            } catch (com.example.petclinic.api.common.validation.ValidationSupportException e) {
                return ApiResponses.badRequest("BAD_REQUEST", e.getMessage());
            } catch (OwnerValidationException e) {
                log.warn("[OwnersCreate] Validation conflict: {}", e.getMessage());
                MetricsSupport.increment("OwnersCreateConflictCount", "Owners", "POST_/owners");
                return ApiResponses.conflict("DUPLICATE_TELEPHONE", e.getMessage());
            } catch (Exception e) {
                log.error("[OwnersCreate] Internal error", e);
                return ApiResponses.serverError("SERVER_ERROR", "Internal error");
            }
        } finally {
            MetricsSupport.publishTimer("OwnersCreateLatencyMs", MetricsSupport.endTimer(handlerStart), "Owners", "POST_/owners");
        }
    }

    // Local test entrypoint (optional)
    public static void main(String[] args) {
        // For local debug one might wire a minimal ApplicationContext
    }
}

// DTOs & helper classes (could be moved to shared lib if reused)
record CreateOwnerRequest(
    @NotBlank @Size(max=30) String firstName,
    @NotBlank @Size(max=30) String lastName,
    @Size(max=255) String address,
    @Size(max=80) String city,
    @Size(max=20) String telephone) {}

record OwnerDto(Long id, String firstName, String lastName, String address, String city, String telephone) {
    static OwnerDto from(Owner o) {
        return new OwnerDto(o.getId(), o.getFirstName(), o.getLastName(), o.getAddress(), o.getCity(), o.getTelephone());
    }
}

@Configuration
@RequiredArgsConstructor
class OwnerServiceConfiguration {
    private final com.example.petclinic.domain.owner.OwnerRepository ownerRepository;
    @Bean
    OwnerService ownerService(jakarta.validation.Validator validator) {
        return new OwnerService(ownerRepository, validator);
    }
}
