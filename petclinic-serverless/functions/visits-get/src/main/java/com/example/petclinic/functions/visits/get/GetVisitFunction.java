package com.example.petclinic.functions.visits.get;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.example.petclinic.domain.visit.Visit;
import com.example.petclinic.domain.visit.VisitService;
import com.example.petclinic.api.common.http.ApiResponses;
import com.example.petclinic.api.common.metrics.MetricsSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.tracing.Tracing;

import java.time.LocalDate;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Component
public class GetVisitFunction implements Function<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private final VisitService visitService;

    public GetVisitFunction(VisitService visitService) {
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
                Map<String, String> pathParams = event.getPathParameters();
                Long ownerId = Long.parseLong(pathParams.getOrDefault("ownerId", "-1"));
                Long visitId = Long.parseLong(pathParams.getOrDefault("visitId", "-1"));

                Visit visit = visitService.getByIdAndOwnerId(visitId, ownerId);
                MetricsSupport.increment("VisitsGetCount", "Visits", "GET_/visits/{id}");
                MetricsSupport.publishTimer("VisitsServiceGetLatencyMs", MetricsSupport.endTimer(serviceStart), "Visits", "GET_/visits/{id}");

                log.info("[VisitsGet] Visit retrieved id={} ownerId={}", visitId, ownerId);
                return ApiResponses.ok(VisitDto.from(visit));
            } catch (com.example.petclinic.domain.visit.VisitNotFoundException e) {
                log.warn("[VisitsGet] Visit not found: {}", e.getMessage());
                return ApiResponses.notFound("NOT_FOUND", e.getMessage());
            } catch (Exception e) {
                log.error("[VisitsGet] Internal error", e);
                return ApiResponses.serverError("SERVER_ERROR", "Internal error");
            }
        } finally {
            MetricsSupport.publishTimer("VisitsGetLatencyMs", MetricsSupport.endTimer(handlerStart), "Visits", "GET_/visits/{id}");
        }
    }
}

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
