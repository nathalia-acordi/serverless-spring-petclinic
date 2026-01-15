package com.example.petclinic.functions.visits.delete;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
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

import java.util.Map;
import java.util.function.Function;

@Slf4j
@Component
public class DeleteVisitFunction implements Function<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private final VisitService visitService;

    public DeleteVisitFunction(VisitService visitService) {
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

                visitService.deleteByIdAndOwnerId(visitId, ownerId);
                MetricsSupport.increment("VisitsDeletedCount", "Visits", "DELETE_/visits/{id}");
                MetricsSupport.publishTimer("VisitsServiceDeleteLatencyMs", MetricsSupport.endTimer(serviceStart), "Visits", "DELETE_/visits/{id}");

                log.info("[VisitsDelete] Visit deleted id={} ownerId={}", visitId, ownerId);
                return ApiResponses.noContent();
            } catch (Exception e) {
                log.error("[VisitsDelete] Internal error", e);
                return ApiResponses.serverError("SERVER_ERROR", "Internal error");
            }
        } finally {
            MetricsSupport.publishTimer("VisitsDeleteLatencyMs", MetricsSupport.endTimer(handlerStart), "Visits", "DELETE_/visits/{id}");
        }
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
