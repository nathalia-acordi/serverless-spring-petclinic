package com.example.petclinic.functions.owners.delete;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.example.petclinic.api.common.http.ApiResponses;
import com.example.petclinic.api.common.metrics.MetricsSupport;
import com.example.petclinic.domain.owner.OwnerNotFoundException;
import com.example.petclinic.domain.owner.OwnerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.tracing.Tracing;

import java.util.Map;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeleteOwnerFunction implements Function<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private final OwnerService service;

    @Override
    @Logging(logEvent = true)
    @Tracing(namespace = "Petclinic")
    public APIGatewayV2HTTPResponse apply(APIGatewayV2HTTPEvent event) {
        long handlerStart = MetricsSupport.startTimer();
        try {
            try {
                if (event == null || event.getPathParameters() == null) {
                    return ApiResponses.badRequest("BAD_REQUEST", "Missing id");
                }
                Map<String,String> path = event.getPathParameters();
                String idStr = path.get("id");
                if (idStr == null || idStr.isBlank()) {
                    return ApiResponses.badRequest("BAD_REQUEST", "Missing id");
                }
                Long id;
                try { id = Long.valueOf(idStr); } catch (NumberFormatException e) { return ApiResponses.badRequest("BAD_REQUEST", "Invalid id"); }
                long serviceStart = MetricsSupport.startTimer();
                service.delete(id);
                // New standardized success counter (keep legacy OwnersDeletedCount for backward compatibility)
                MetricsSupport.increment("OwnersDeleteSuccessCount", "Owners", "DELETE_/owners/{id}");
                MetricsSupport.increment("OwnersDeletedCount", "Owners", "DELETE_/owners/{id}");
                MetricsSupport.publishTimer("OwnersServiceDeleteLatencyMs", MetricsSupport.endTimer(serviceStart), "Owners", "DELETE_/owners/{id}");
                return ApiResponses.ok(Map.of("deleted", true));
            } catch (com.example.petclinic.domain.owner.OwnerNotFoundException e) {
                MetricsSupport.increment("OwnersDeleteNotFoundCount", "Owners", "DELETE_/owners/{id}");
                return ApiResponses.notFound("NOT_FOUND", "Owner not found");
            } catch (Exception e) {
                log.error("[OwnersDelete] Internal error", e);
                return ApiResponses.serverError("SERVER_ERROR", "Internal error");
            }
        } finally {
            MetricsSupport.publishTimer("OwnersDeleteLatencyMs", MetricsSupport.endTimer(handlerStart), "Owners", "DELETE_/owners/{id}");
        }
    }
}

