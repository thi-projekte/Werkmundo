package de.werkmundo.invoice.infrastructure.offer;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/offers")
@Produces(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "offer-service")
public interface OfferServiceClient {

    @GET
    @Path("/{businessKey}")
    OfferResponse getOffer(
            @PathParam("businessKey") String businessKey,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization
    );
}