package org.anta.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<RuntimeException> {

    @Override
    public Response toResponse(RuntimeException exception) {
        int status = 500;

        if (exception instanceof InsufficientStockException) {
            status = 409;
        } else if (exception instanceof ReservationException) {
            status = 400;
        }

        return Response.status(status)
                .entity(Map.of(
                        "error", exception.getMessage() == null ? "Internal server error" : exception.getMessage()
                ))
                .build();
    }
}