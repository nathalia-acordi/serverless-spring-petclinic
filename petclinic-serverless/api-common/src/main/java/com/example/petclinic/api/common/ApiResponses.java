package com.example.petclinic.api.common;

/** Helper static factory methods */
public final class ApiResponses {
    private ApiResponses() {}

    public static <T> ApiResponse<T> ok(T body) { return new ApiResponse<>(200, body, null); }
    public static <T> ApiResponse<T> created(T body) { return new ApiResponse<>(201, body, null); }
    public static <T> ApiResponse<T> noContent() { return new ApiResponse<>(204, null, null); }
    public static <T> ApiResponse<T> badRequest(String msg) { return new ApiResponse<>(400, null, new ErrorResponse("BAD_REQUEST", msg)); }
    public static <T> ApiResponse<T> notFound(String msg) { return new ApiResponse<>(404, null, new ErrorResponse("NOT_FOUND", msg)); }
    public static <T> ApiResponse<T> conflict(String msg) { return new ApiResponse<>(409, null, new ErrorResponse("CONFLICT", msg)); }
    public static <T> ApiResponse<T> serverError(String msg) { return new ApiResponse<>(500, null, new ErrorResponse("SERVER_ERROR", msg)); }
}
