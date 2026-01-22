package com.example.petclinic.functions.vets.get;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.example.petclinic.domain.vet.Specialty;
import com.example.petclinic.domain.vet.Vet;
import com.example.petclinic.domain.vet.VetService;
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
import java.util.stream.Collectors;

@Slf4j
@Component
public class GetVetFunction implements Function<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private final VetService vetService;

    public GetVetFunction(VetService vetService) {
        this.vetService = vetService;
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
                Long vetId = Long.parseLong(pathParams.getOrDefault("vetId", "-1"));

                Vet vet = vetService.get(vetId);
                MetricsSupport.increment("VetsGetCount", "Vets", "GET_/vets/{id}");
                MetricsSupport.publishTimer("VetsServiceGetLatencyMs", MetricsSupport.endTimer(serviceStart), "Vets", "GET_/vets/{id}");

                log.info("[VetsGet] Vet retrieved id={}", vetId);
                return ApiResponses.ok(VetDto.from(vet));
            } catch (com.example.petclinic.domain.vet.VetNotFoundException e) {
                log.warn("[VetsGet] Vet not found: {}", e.getMessage());
                return ApiResponses.notFound("NOT_FOUND", e.getMessage());
            } catch (Exception e) {
                log.error("[VetsGet] Internal error", e);
                return ApiResponses.serverError("SERVER_ERROR", "Internal error");
            }
        } finally {
            MetricsSupport.publishTimer("VetsGetLatencyMs", MetricsSupport.endTimer(handlerStart), "Vets", "GET_/vets/{id}");
        }
    }
}

record SpecialtyDto(Long id, String name) {
    static SpecialtyDto from(Specialty s) {
        return new SpecialtyDto(s.getId(), s.getName());
    }
}

record VetDto(Long id, String firstName, String lastName, List<SpecialtyDto> specialties) {
    static VetDto from(Vet v) {
        List<SpecialtyDto> specialtyDtos = v.getSpecialties() != null 
            ? v.getSpecialties().stream().map(SpecialtyDto::from).collect(Collectors.toList())
            : List.of();
        return new VetDto(
                v.getId(),
                v.getFirstName(),
                v.getLastName(),
                specialtyDtos);
    }
}

@Configuration
@RequiredArgsConstructor
class VetServiceConfiguration {
    private final com.example.petclinic.domain.vet.VetRepository vetRepository;

    @Bean
    public VetService vetService() {
        return new VetService(vetRepository);
    }
}
