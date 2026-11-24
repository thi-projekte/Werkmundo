package de.werkmundo.offer.ai;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST-Client für den synchronen Endpunkt {@code POST /ai/analyze} des ai-service.
 *
 * <p>Wird nur im Direktmodus ({@code AI_DIRECT_ENABLED=true}) genutzt, solange es online
 * keine Process Engine gibt. Der ai-service liegt im internen Netz und verlangt dort kein Token.
 */
@Path("/ai")
@RegisterRestClient(configKey = "ai-service")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface AiServiceRestClient {

    @POST
    @Path("/analyze")
    AiAnalyzeResult analyze(AiAnalyzeRequest request,
                            @HeaderParam("X-Handwerker-Id") String handwerkerId);
}
