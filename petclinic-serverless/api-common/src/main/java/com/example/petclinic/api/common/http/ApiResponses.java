package com.example.petclinic.api.common.http;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public final class ApiResponses {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  public static APIGatewayV2HTTPResponse ok(Object body) { return build(200, body); }
  public static APIGatewayV2HTTPResponse created(Object body) { return build(201, body); }
  public static APIGatewayV2HTTPResponse noContent() { return build(204, null); }
  public static APIGatewayV2HTTPResponse badRequest(String code, String message) {
    return build(400, Map.of("code", code, "message", message));
  }
  public static APIGatewayV2HTTPResponse notFound(String code, String message) {
    return build(404, Map.of("code", code, "message", message));
  }
  public static APIGatewayV2HTTPResponse conflict(String code, String message) {
    return build(409, Map.of("code", code, "message", message));
  }
  public static APIGatewayV2HTTPResponse serverError(String code, String message) {
    return build(500, Map.of("code", code, "message", message));
  }

  private static APIGatewayV2HTTPResponse build(int status, Object bodyObj) {
    try {
      String body = (bodyObj instanceof String) ? (String) bodyObj : MAPPER.writeValueAsString(bodyObj);
      return APIGatewayV2HTTPResponse.builder()
          .withStatusCode(status)
          .withHeaders(Map.of("Content-Type", "application/json"))
          .withBody(body)
          .build();
    } catch (Exception e) {
      return APIGatewayV2HTTPResponse.builder()
          .withStatusCode(500)
          .withHeaders(Map.of("Content-Type", "application/json"))
          .withBody("{\"code\":\"SERIALIZATION_ERROR\",\"message\":\"Failed to serialize body\"}")
          .build();
    }
  }

  private ApiResponses() {}
}
