package com.example.petclinic.functions.owners.list;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.example.petclinic.api.common.http.ApiResponses;
import com.example.petclinic.api.common.metrics.MetricsSupport;
import com.example.petclinic.domain.owner.Owner;
import com.example.petclinic.domain.owner.OwnerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.tracing.Tracing;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class ListOwnersFunction implements Function<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private final OwnerService service;

    @Override
    @Logging(logEvent = true)
    @Tracing(namespace = "Petclinic")
    public APIGatewayV2HTTPResponse apply(APIGatewayV2HTTPEvent event) {
        long handlerStart = MetricsSupport.startTimer();
        try {
            try {
                int page = 0;
                int size = 20;
                if (event != null && event.getQueryStringParameters() != null) {
                    Map<String,String> q = event.getQueryStringParameters();
                    if (q.get("page") != null) page = Integer.parseInt(q.get("page"));
                    if (q.get("size") != null) size = Integer.parseInt(q.get("size"));
                }
                long serviceStart = MetricsSupport.startTimer();
                List<OwnerDto> owners = service.list(page, size).stream().map(OwnerDto::from).toList();
                MetricsSupport.increment("OwnersListedCount", "Owners", "GET_/owners");
                MetricsSupport.publishTimer("OwnersServiceListLatencyMs", MetricsSupport.endTimer(serviceStart), "Owners", "GET_/owners");
                return ApiResponses.ok(owners);
            } catch (NumberFormatException e) {
                return ApiResponses.badRequest("BAD_REQUEST", "Invalid paging parameters");
            } catch (Exception e) {
                log.error("[OwnersList] Internal error", e);
                return ApiResponses.serverError("SERVER_ERROR", "Internal error");
            }
        } finally {
            MetricsSupport.publishTimer("OwnersListLatencyMs", MetricsSupport.endTimer(handlerStart), "Owners", "GET_/owners");
        }
    }
}

record OwnerDto(Long id, String firstName, String lastName, String address, String city, String telephone) {
    static OwnerDto from(Owner o) { return new OwnerDto(o.getId(), o.getFirstName(), o.getLastName(), o.getAddress(), o.getCity(), o.getTelephone()); }
}
