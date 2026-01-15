package com.example.petclinic.functions.visits.update;

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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Component
public class UpdateVisitFunction implements Function<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private final VisitService visitService;
    private final ObjectMapper mapper = new ObjectMapper();

    public UpdateVisitFunction(VisitService visitService) {
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
                Long visitId = Long.parseLong(pathParams.getOrDefault("visitId", "-1"));

                UpdateVisitRequest request = mapper.readValue(event.getBody(), UpdateVisitRequest.class);

                Visit toUpdate = new Visit();
                toUpdate.setId(visitId);
                toUpdate.setOwnerId(ownerId);
                if (request.visitDate() != null) {
                    toUpdate.setVisitDate(LocalDate.parse(request.visitDate(), DateTimeFormatter.ISO_LOCAL_DATE));
                }
                if (request.description() != null) {
                    toUpdate.setDescription(request.description());
                }

                Visit updated = visitService.update(visitId, ownerId, toUpdate);
                MetricsSupport.increment("VisitsUpdatedCount", "Visits", "PUT_/visits/{id}");
                MetricsSupport.publishTimer("VisitsServiceUpdateLatencyMs", MetricsSupport.endTimer(serviceStart), "Visits", "PUT_/visits/{id}");

                log.info("[VisitsUpdate] Visit updated id={} ownerId={}", visitId, ownerId);
                return ApiResponses.ok(VisitDto.from(updated));
            } catch (com.example.petclinic.domain.visit.VisitNotFoundException e) {
                log.warn("[VisitsUpdate] Visit not found: {}", e.getMessage());
                return ApiResponses.notFound("NOT_FOUND", e.getMessage());
            } catch (VisitValidationException e) {
                log.warn("[VisitsUpdate] Validation error: {}", e.getMessage());
                return ApiResponses.badRequest("VALIDATION_ERROR", e.getMessage());
            } catch (Exception e) {
                log.error("[VisitsUpdate] Internal error", e);
                return ApiResponses.serverError("SERVER_ERROR", "Internal error");
            }
        } finally {
            MetricsSupport.publishTimer("VisitsUpdateLatencyMs", MetricsSupport.endTimer(handlerStart), "Visits", "PUT_/visits/{id}");
        }
    }
}

record UpdateVisitRequest(String visitDate, String description) {}

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
