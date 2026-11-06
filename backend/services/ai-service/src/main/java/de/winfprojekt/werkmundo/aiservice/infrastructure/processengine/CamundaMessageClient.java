package de.winfprojekt.werkmundo.aiservice.infrastructure.processengine;

import de.winfprojekt.werkmundo.aiservice.infrastructure.auth.TechnicalTokenFilter;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST-Client zur Process Engine (Basis-URL endet auf {@code /engine-rest}, Konfiguration
 * {@code quarkus.rest-client."camunda-engine".url}). Erfolgreiche Korrelation: HTTP 200/204.
 * HTTP 400 bedeutet meist: die Prozessinstanz wartet (noch) nicht auf die Message.
 */
@RegisterRestClient(configKey = "camunda-engine")
@RegisterProvider(TechnicalTokenFilter.class)
@Path("/message")
public interface CamundaMessageClient {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    Response correlate(CamundaCorrelationRequest request);
}
