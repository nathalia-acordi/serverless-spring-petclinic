package com.example.petclinic.functions.owners.update;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.example.petclinic.api.common.http.ApiResponses;
import com.example.petclinic.api.common.Json;
import com.example.petclinic.api.common.metrics.MetricsSupport;
import com.example.petclinic.api.common.validation.ValidationSupport;
import com.example.petclinic.domain.owner.Owner;
import com.example.petclinic.domain.owner.OwnerNotFoundException;
import com.example.petclinic.domain.owner.OwnerService;
import com.example.petclinic.domain.owner.OwnerValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.tracing.Tracing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateOwnerFunction implements Function<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private final OwnerService service;

    @Override
    @Logging(logEvent = true)
    @Tracing(namespace = "Petclinic")
    public APIGatewayV2HTTPResponse apply(APIGatewayV2HTTPEvent event) {
        long handlerStart = MetricsSupport.startTimer();
        String endpoint = "PUT_/owners/{id}";
        ObjectMapper mapper = Json.mapper();
        Long pathId = null;
        try {
            // 1. Path ID
            if (event == null || event.getPathParameters() == null) {
                MetricsSupport.increment("OwnersUpdateBadRequestCount", "Owners", endpoint);
                return ApiResponses.badRequest("VALIDATION_ERROR", "Missing path parameters");
            }
            String rawPathId = event.getPathParameters().get("id");
            if (rawPathId == null || rawPathId.isBlank()) {
                MetricsSupport.increment("OwnersUpdateBadRequestCount", "Owners", endpoint);
                return ApiResponses.badRequest("VALIDATION_ERROR", "Missing id in path");
            }
            try { pathId = Long.valueOf(rawPathId); } catch (NumberFormatException nfe) {
                MetricsSupport.increment("OwnersUpdateBadRequestCount", "Owners", endpoint);
                return ApiResponses.badRequest("VALIDATION_ERROR", "Invalid path id");
            }

            // 2. Content-Type (case-insensitive startsWith application/json)
            String contentType = null;
            if (event.getHeaders() != null) {
                for (Map.Entry<String,String> h : event.getHeaders().entrySet()) {
                    if (h.getKey() != null && h.getKey().equalsIgnoreCase("content-type")) { contentType = h.getValue(); break; }
                }
            }
            if (contentType != null) {
                String lower = contentType.toLowerCase(Locale.ROOT).trim();
                if (!lower.startsWith("application/json")) {
                    MetricsSupport.increment("OwnersUpdateBadRequestCount", "Owners", endpoint);
                    return ApiResponses.badRequest("VALIDATION_ERROR", "Unsupported Content-Type");
                }
            }

            // 3. Raw body normalization (base64 handling)
            String rawBody = event.getBody();
            if (rawBody == null) rawBody = "";
            if (Boolean.TRUE.equals(event.getIsBase64Encoded())) {
                try {
                    byte[] decoded = Base64.getDecoder().decode(rawBody);
                    rawBody = new String(decoded, StandardCharsets.UTF_8);
                } catch (IllegalArgumentException iae) {
                    // Fallback: assume API Gateway flag incorreto, usa corpo original
                    log.debug("{\"event\":\"OwnersUpdate\",\"stage\":\"base64_fallback\",\"msg\":\"Decode failed, using raw body\",\"error\":\"{}\"}", iae.getMessage());
                }
            }
            String body = rawBody.trim();
            if (body.isEmpty()) {
                MetricsSupport.increment("OwnersUpdateBadRequestCount", "Owners", endpoint);
                return ApiResponses.badRequest("VALIDATION_ERROR", "Empty body");
            }
            if (log.isDebugEnabled()) {
                String preview = body.length() > 200 ? body.substring(0,200) + "..." : body;
                log.debug("{\"event\":\"OwnersUpdate\",\"debug\":\"body_preview\",\"len\":{},\"isB64\":{},\"contentType\":\"{}\",\"preview\":\"{}\"}", body.length(), event.getIsBase64Encoded(), contentType, preview.replace('"',' '));
            }

            // 4. Deserialize
            UpdateOwnerRequest req;
            try {
                req = mapper.readValue(body, UpdateOwnerRequest.class);
            } catch (JsonProcessingException jpe) {
                MetricsSupport.increment("OwnersUpdateBadRequestCount", "Owners", endpoint);
                log.warn("{\"event\":\"OwnersUpdate\",\"stage\":\"json_parse\",\"error\":\"{}\"}", jpe.getOriginalMessage());
                return ApiResponses.badRequest("VALIDATION_ERROR", "Malformed JSON body");
            }

            // 5. Body id mismatch check (optional id)
            if (req.getId() != null && !req.getId().equals(pathId)) {
                MetricsSupport.increment("OwnersUpdateBadRequestCount", "Owners", endpoint);
                return ApiResponses.badRequest("VALIDATION_ERROR", "Path id and body id mismatch");
            }

            // 6. Bean + custom validation
            try { ValidationSupport.validate(req); } catch (com.example.petclinic.api.common.validation.ValidationSupportException ve) {
                MetricsSupport.increment("OwnersUpdateBadRequestCount", "Owners", endpoint);
                log.warn("{\"event\":\"OwnersUpdate\",\"id\":{},\"validation\":\"{}\"}", pathId, ve.getMessage());
                return ApiResponses.badRequest("VALIDATION_ERROR", ve.getMessage());
            }

            long serviceStart = MetricsSupport.startTimer();
            try {
        Owner updated = service.update(pathId, Owner.builder()
            .firstName(req.getFirstName())
            .lastName(req.getLastName())
            .address(req.getAddress())
            .city(req.getCity())
            .telephone(req.getTelephone())
                        .build());
                MetricsSupport.increment("OwnersUpdateSuccessCount", "Owners", endpoint);
                MetricsSupport.publishTimer("OwnersServiceUpdateLatencyMs", MetricsSupport.endTimer(serviceStart), "Owners", endpoint);
                log.info("{\"event\":\"OwnersUpdate\",\"status\":\"success\",\"id\":{},\"firstName\":\"{}\",\"lastName\":\"{}\"}", updated.getId(), updated.getFirstName(), updated.getLastName());
                return ApiResponses.ok(OwnerDto.from(updated));
            } catch (OwnerNotFoundException e) {
                MetricsSupport.increment("OwnersUpdateNotFoundCount", "Owners", endpoint);
                log.warn("{\"event\":\"OwnersUpdate\",\"status\":\"not_found\",\"id\":{},\"message\":\"Owner not found\"}", pathId);
                return ApiResponses.notFound("OWNER_NOT_FOUND", "Owner not found");
            } catch (OwnerValidationException e) {
                MetricsSupport.increment("OwnersUpdateConflictCount", "Owners", endpoint);
                log.warn("{\"event\":\"OwnersUpdate\",\"status\":\"conflict\",\"id\":{},\"message\":\"{}\"}", pathId, e.getMessage());
                return ApiResponses.conflict("DUPLICATE_TELEPHONE", e.getMessage());
            }
        } catch (Exception e) {
            MetricsSupport.increment("OwnersUpdateErrorCount", "Owners", endpoint);
            log.error("{\"event\":\"OwnersUpdate\",\"status\":\"error\",\"id\":%s,\"message\":\"%s\"}".formatted(String.valueOf(pathId), e.getMessage()), e);
            return ApiResponses.serverError("SERVER_ERROR", "Internal error");
        } finally {
            MetricsSupport.publishTimer("OwnersUpdateLatencyMs", MetricsSupport.endTimer(handlerStart), "Owners", endpoint);
        }
    }
}
class UpdateOwnerRequest {
    private Long id; private String firstName; private String lastName; private String address; private String city; private String telephone;
    public UpdateOwnerRequest() {}
    public UpdateOwnerRequest(Long id, String firstName, String lastName, String address, String city, String telephone) {
        this.id = id; this.firstName = firstName; this.lastName = lastName; this.address = address; this.city = city; this.telephone = telephone;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
}
record OwnerDto(Long id, String firstName, String lastName, String address, String city, String telephone) {
    static OwnerDto from(Owner o) { return new OwnerDto(o.getId(), o.getFirstName(), o.getLastName(), o.getAddress(), o.getCity(), o.getTelephone()); }
}
