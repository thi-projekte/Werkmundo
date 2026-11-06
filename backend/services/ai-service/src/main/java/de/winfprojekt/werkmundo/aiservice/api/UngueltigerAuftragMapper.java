package de.winfprojekt.werkmundo.aiservice.api;

import de.winfprojekt.werkmundo.aiservice.api.dto.ErrorResponse;
import de.winfprojekt.werkmundo.aiservice.domain.UngueltigerAuftragException;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

/** Unvollstaendiger oder nicht zuordenbarer Payload → HTTP 400. */
@Provider
public class UngueltigerAuftragMapper implements ExceptionMapper<UngueltigerAuftragException> {

    private static final Logger LOG = Logger.getLogger(UngueltigerAuftragMapper.class);

    @Override
    public Response toResponse(UngueltigerAuftragException e) {
        LOG.warnf("Ungueltiger KI-Auftrag: %s", e.getMessage());
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
    }
}
