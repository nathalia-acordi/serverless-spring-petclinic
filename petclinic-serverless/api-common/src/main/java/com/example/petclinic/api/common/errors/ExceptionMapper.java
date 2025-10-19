package com.example.petclinic.api.common.errors;

import com.example.petclinic.api.common.ApiResponse;
import com.example.petclinic.api.common.ApiResponses;
import com.example.petclinic.api.common.validation.ValidationSupportException;
import com.example.petclinic.domain.owner.OwnerNotFoundException;

import java.util.function.Supplier;

/** Central mapping from exceptions to ApiResponses. Extend with new domain exceptions as needed. */
public final class ExceptionMapper {
    private ExceptionMapper() {}

    public static <T> ApiResponse<T> map(Throwable t) {
        if (t instanceof ValidationSupportException v) {
            return ApiResponses.badRequest(v.getMessage());
        }
        if (t instanceof OwnerNotFoundException) {
            return ApiResponses.notFound("Owner not found");
        }
        return ApiResponses.serverError("Internal error");
    }

    public static <T> ApiResponse<T> execute(Supplier<ApiResponse<T>> supplier) {
        try { return supplier.get(); }
        catch (Throwable t) { return map(t); }
    }
}
