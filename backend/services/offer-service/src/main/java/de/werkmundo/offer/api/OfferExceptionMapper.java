package de.werkmundo.offer.api;

import de.werkmundo.offer.api.dto.ErrorResponse;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.NoSuchElementException;

@Provider
public class OfferExceptionMapper implements ExceptionMapper<RuntimeException> {
    @Override
    public Response toResponse(RuntimeException exception) {
        if (exception instanceof WebApplicationException webException) {
            return webException.getResponse();
        }
        if (exception instanceof NoSuchElementException) {
            return error(Response.Status.NOT_FOUND, "Offer was not found");
        }
        if (exception instanceof SecurityException) {
            return error(Response.Status.UNAUTHORIZED, "Invalid or expired access token");
        }
        if (exception instanceof IllegalStateException) {
            return error(Response.Status.CONFLICT, "The offer cannot be changed in its current state");
        }
        if (exception instanceof IllegalArgumentException) {
            return error(Response.Status.BAD_REQUEST, "Invalid request");
        }
        return error(Response.Status.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private Response error(Response.Status status, String message) {
        return Response.status(status)
                .entity(new ErrorResponse(status.getStatusCode(), message))
                .build();
    }
}
