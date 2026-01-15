package com.example.petclinic.functions.visits.list;

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

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Component
public class ListVisitsFunction implements Function<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private final VisitService visitService;

    public ListVisitsFunction(VisitService visitService) {
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
                Long petId = Long.parseLong(pathParams.getOrDefault("petId", "-1"));

                List<Visit> visits = visitService.listByPet(ownerId, petId);
                MetricsSupport.increment("VisitsListCount", "Visits", "GET_/visits");
                MetricsSupport.publishTimer("VisitsServiceListLatencyMs", MetricsSupport.endTimer(serviceStart), "Visits", "GET_/visits");

                log.info("[VisitsList] Retrieved {} visits for ownerId={} petId={}", visits.size(), ownerId, petId);
                List<VisitDto> dtos = visits.stream().map(VisitDto::from).toList();
                return ApiResponses.ok(dtos);
            } catch (Exception e) {
                log.error("[VisitsList] Internal error", e);
                return ApiResponses.serverError("SERVER_ERROR", "Internal error");
            }
        } finally {
            MetricsSupport.publishTimer("VisitsListLatencyMs", MetricsSupport.endTimer(handlerStart), "Visits", "GET_/visits");
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
