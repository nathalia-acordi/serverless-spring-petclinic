package com.example.petclinic.functions.visits.create;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.example.petclinic.domain.visit.Visit;
import com.example.petclinic.domain.visit.VisitService;
import com.example.petclinic.domain.visit.VisitValidationException;
import com.example.petclinic.api.common.http.ApiResponses;
import com.example.petclinic.api.common.metrics.MetricsSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.tracing.Tracing;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Component
public class CreateVisitFunction implements Function<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private final VisitService visitService;
    private final ObjectMapper mapper = new ObjectMapper();

    public CreateVisitFunction(VisitService visitService) {
        this.visitService = visitService;
    }

    @Override
    @Logging(logEvent = true)
    @Tracing(namespace = "Petclinic")
    public APIGatewayV2HTTPResponse apply(APIGatewayV2HTTPEvent event) {
        long handlerStart = MetricsSupport.startTimer();
        try {
            long serviceStart = MetricsSupport.startTimer();
            try {
                if (event == null || event.getBody() == null || event.getBody().isBlank()) {
                    return ApiResponses.badRequest("BAD_REQUEST", "Empty body");
                }

                Map<String, String> pathParams = event.getPathParameters();
                Long ownerId = Long.parseLong(pathParams.getOrDefault("ownerId", "-1"));
                Long petId = Long.parseLong(pathParams.getOrDefault("petId", "-1"));

                CreateVisitRequest request = mapper.readValue(event.getBody(), CreateVisitRequest.class);

                Visit toCreate = new Visit(
                        ownerId,
                        petId,
                        LocalDate.parse(request.visitDate(), DateTimeFormatter.ISO_LOCAL_DATE),
                        request.description());

                Visit saved = visitService.create(toCreate);
                MetricsSupport.increment("VisitsCreatedCount", "Visits", "POST_/visits");
                MetricsSupport.publishTimer("VisitsServiceCreateLatencyMs", MetricsSupport.endTimer(serviceStart), "Visits", "POST_/visits");

                log.info("[VisitsCreate] Visit created id={} ownerId={} petId={}", saved.getId(), ownerId, petId);
                return ApiResponses.created(VisitDto.from(saved));
            } catch (VisitValidationException e) {
                log.warn("[VisitsCreate] Validation error: {}", e.getMessage());
                MetricsSupport.increment("VisitsCreateConflictCount", "Visits", "POST_/visits");
                return ApiResponses.badRequest("VALIDATION_ERROR", e.getMessage());
            } catch (Exception e) {
                log.error("[VisitsCreate] Internal error", e);
                return ApiResponses.serverError("SERVER_ERROR", "Internal error");
            }
        } finally {
            MetricsSupport.publishTimer("VisitsCreateLatencyMs", MetricsSupport.endTimer(handlerStart), "Visits", "POST_/visits");
        }
    }
}

record CreateVisitRequest(
    @NotNull String visitDate,
    @NotBlank String description) {}

record VisitDto(Long id, Long ownerId, Long petId, String visitDate, String description) {
    static VisitDto from(Visit v) {
        return new VisitDto(
                v.getId(),
                v.getOwnerId(),
                v.getPetId(),
                v.getVisitDate().toString(),
                v.getDescription());
    }
}

@Configuration
@RequiredArgsConstructor
class VisitServiceConfiguration {
    private final com.example.petclinic.domain.visit.VisitRepository visitRepository;

    @Bean
    public VisitService visitService() {
        return new VisitService(visitRepository);
    }
}
