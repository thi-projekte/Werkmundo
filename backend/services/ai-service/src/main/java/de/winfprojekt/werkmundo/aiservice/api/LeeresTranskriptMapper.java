package de.winfprojekt.werkmundo.aiservice.api;

import de.winfprojekt.werkmundo.aiservice.api.dto.ErrorResponse;
import de.winfprojekt.werkmundo.aiservice.domain.LeeresTranskriptException;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Keine Sprache in der Aufnahme erkannt → HTTP 422. */
@Provider
public class LeeresTranskriptMapper implements ExceptionMapper<LeeresTranskriptException> {

    @Override
    public Response toResponse(LeeresTranskriptException e) {
        return Response.status(422)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
    }
}
