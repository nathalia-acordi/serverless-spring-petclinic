package com.example.petclinic.functions.vets.list;

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
public class ListVetsFunction implements Function<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private final VetService vetService;

    public ListVetsFunction(VetService vetService) {
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
                List<Vet> vets = vetService.list(0, 999);
                MetricsSupport.increment("VetsListCount", "Vets", "GET_/vets");
                MetricsSupport.publishTimer("VetsServiceListLatencyMs", MetricsSupport.endTimer(serviceStart), "Vets", "GET_/vets");

                log.info("[VetsList] Retrieved {} vets", vets.size());
                List<VetDto> dtos = vets.stream().map(VetDto::from).collect(Collectors.toList());
                return ApiResponses.ok(dtos);
            } catch (Exception e) {
                log.error("[VetsList] Internal error", e);
                return ApiResponses.serverError("SERVER_ERROR", "Internal error");
            }
        } finally {
            MetricsSupport.publishTimer("VetsListLatencyMs", MetricsSupport.endTimer(handlerStart), "Vets", "GET_/vets");
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
