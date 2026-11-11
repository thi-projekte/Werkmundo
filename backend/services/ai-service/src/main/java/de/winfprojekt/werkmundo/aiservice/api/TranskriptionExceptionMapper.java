package de.winfprojekt.werkmundo.aiservice.api;

import de.winfprojekt.werkmundo.aiservice.api.dto.ErrorResponse;
import de.winfprojekt.werkmundo.aiservice.application.TranskriptionException;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

/** Fehler beim Speech-to-Text-Anbieter → HTTP 502 (Details nur im Log). */
@Provider
public class TranskriptionExceptionMapper implements ExceptionMapper<TranskriptionException> {

    private static final Logger LOG = Logger.getLogger(TranskriptionExceptionMapper.class);

    @Override
    public Response toResponse(TranskriptionException e) {
        LOG.errorf("Transkription fehlgeschlagen: %s", e.getMessage());
        return Response.status(Response.Status.BAD_GATEWAY)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse("Die Spracherkennung ist gerade nicht erreichbar. Bitte später erneut versuchen."))
                .build();
    }
}
