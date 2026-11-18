package de.werkmundo.invoice.infrastructure.user;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "user-service")
public interface UserServiceClient {

    @GET
    @Path("/customers/{id}")
    CustomerResponse getCustomer(
            @PathParam("id") Long id,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization
    );
}