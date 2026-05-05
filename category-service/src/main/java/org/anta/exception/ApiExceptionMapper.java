package org.anta.exception;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ApiExceptionMapper implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable ex) {
        if (ex instanceof NotFoundException) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ApiError("NOT_FOUND", ex.getMessage()))
                    .build();
        }

        if (ex instanceof ConflictException) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ApiError("CONFLICT", ex.getMessage()))
                    .build();
        }

        if (ex instanceof ConstraintViolationException) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiError("BAD_REQUEST", ex.getMessage()))
                    .build();
        }

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ApiError(
                        "INTERNAL_SERVER_ERROR",
                        ex.getMessage() == null ? "Internal server error" : ex.getMessage()
                ))
                .build();
    }
}