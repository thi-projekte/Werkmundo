package de.winfprojekt.werkmundo.aiservice.infrastructure.llm;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST-Client zur MegaLLM-API. Basis-URL aus {@code quarkus.rest-client."megallm".url}
 * (z.B. {@code https://ai.megallm.io/v1}). Liefert die rohe {@link Response}, damit der
 * {@link MegaLlmAdapter} Statuscodes fuer Retries auswerten kann. Der API-Key wird pro Aufruf
 * als Bearer-Header uebergeben.
 */
@RegisterRestClient(configKey = "megallm")
@Path("/chat/completions")
public interface MegaLlmClient {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response complete(@HeaderParam("Authorization") String authorization, ChatRequest request);
}
