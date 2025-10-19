package com.example.petclinic.api.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/** ObjectMapper singleton helper (thread-safe). */
public final class Json {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private Json() {}

    public static ObjectMapper mapper() { return MAPPER; }

    public static String toJson(Object o) {
        try { return MAPPER.writeValueAsString(o); } catch (JsonProcessingException e) { throw new RuntimeException(e); }
    }
}
