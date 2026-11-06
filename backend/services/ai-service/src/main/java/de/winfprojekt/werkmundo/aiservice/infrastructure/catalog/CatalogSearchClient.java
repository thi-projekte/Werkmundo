package de.winfprojekt.werkmundo.aiservice.infrastructure.catalog;

import de.winfprojekt.werkmundo.aiservice.infrastructure.auth.TechnicalTokenFilter;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST-Client zum Such-Endpoint des catalog-service (gehoert Squad Kunde/Auftrag).
 * Basis-URL aus {@code quarkus.rest-client."catalog-search".url}. {@code X-Handwerker-Id}
 * bestimmt, in wessen Katalog gesucht wird.
 */
@RegisterRestClient(configKey = "catalog-search")
@RegisterProvider(TechnicalTokenFilter.class)
@Path("/catalog/material/search")
public interface CatalogSearchClient {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    CatalogSearchResponse search(@QueryParam("q") String query,
                                 @QueryParam("limit") int limit,
                                 @HeaderParam("X-Handwerker-Id") String handwerkerId);
}
