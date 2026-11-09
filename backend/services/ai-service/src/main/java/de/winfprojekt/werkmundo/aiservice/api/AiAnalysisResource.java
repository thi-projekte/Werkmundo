package de.winfprojekt.werkmundo.aiservice.api;

import de.winfprojekt.werkmundo.aiservice.api.dto.AcceptedResponse;
import de.winfprojekt.werkmundo.aiservice.api.dto.AiAnalysisRequest;
import de.winfprojekt.werkmundo.aiservice.application.AnalyzeVoiceInputUseCase;
import de.winfprojekt.werkmundo.aiservice.domain.AnalyseAuftrag;
import de.winfprojekt.werkmundo.aiservice.domain.ErgebnisKi;
import de.winfprojekt.werkmundo.aiservice.domain.UngueltigerAuftragException;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST-Endpoints des ai-service.
 *
 * <p>Zugriffsschutz wird per Konfiguration gesteuert (HTTP-Permission auf {@code /ai/*},
 * siehe {@code application.properties}): lokal offen, mit {@code AI_SECURITY_ENABLED=true}
 * und {@code AI_AUTH_POLICY=process-engine} nur mit Keycloak-Token der Rolle
 * {@code process-engine}.
 */
@Path("/ai")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AiAnalysisResource {

    private final AnalyzeVoiceInputUseCase useCase;

    public AiAnalysisResource(AnalyzeVoiceInputUseCase useCase) {
        this.useCase = useCase;
    }

    /**
     * Aufruf durch die Process Engine. Antwortet sofort mit 202; das Ergebnis wird im
     * Hintergrund erzeugt und als Message {@code ergebnisKI} per {@code businessKey} korreliert.
     *
     * @param handwerkerId Keycloak-ID des Handwerkers (Mandant fuer die Katalogsuche), optional
     */
    @POST
    @Path("/process")
    public Response process(AiAnalysisRequest request,
                            @HeaderParam("X-Handwerker-Id") String handwerkerId) {
        AnalyseAuftrag auftrag = toAuftrag(request, handwerkerId);
        useCase.startAsync(auftrag);
        return Response.accepted(AcceptedResponse.accepted(auftrag.businessKey())).build();
    }

    /**
     * Gleicher Payload wie {@code /process}, liefert das {@code ergebnisKI} aber direkt in der
     * Antwort (HTTP 200) und spricht die Process Engine nicht an. Fuer lokale Tests und die
     * Frontend-Entwicklung ohne Process Engine.
     */
    @POST
    @Path("/analyze")
    public ErgebnisKi analyze(AiAnalysisRequest request,
                              @HeaderParam("X-Handwerker-Id") String handwerkerId) {
        return useCase.analysiere(toAuftrag(request, handwerkerId));
    }

    private static AnalyseAuftrag toAuftrag(AiAnalysisRequest request, String handwerkerId) {
        if (request == null) {
            throw new UngueltigerAuftragException("Request-Body fehlt.");
        }
        return request.toAuftrag(handwerkerId);
    }
}
