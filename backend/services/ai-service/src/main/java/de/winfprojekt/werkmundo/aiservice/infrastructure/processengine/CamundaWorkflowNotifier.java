package de.winfprojekt.werkmundo.aiservice.infrastructure.processengine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.winfprojekt.werkmundo.aiservice.application.port.WorkflowNotifier;
import de.winfprojekt.werkmundo.aiservice.domain.ErgebnisKi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

/**
 * {@link WorkflowNotifier} fuer die Process Engine (CIB seven / Camunda 7 REST-API).
 *
 * <p>Serialisiert das {@link ErgebnisKi} und korreliert es als Message {@code ergebnisKI} per
 * {@code businessKey}.
 *
 * <p><b>Retry bei HTTP 400:</b> Die Engine legt die Message-Subscription erst an, nachdem sie
 * unsere 202-Antwort verarbeitet hat. Ist das Ergebnis sehr schnell fertig (z.B. Stub ohne
 * API-Key), kann die Message zu frueh kommen. Deshalb wird HTTP 400 einige Male mit kurzer
 * Wartezeit wiederholt ({@code ai.processengine.correlation.max-attempts} / {@code .wait-ms}).
 */
@ApplicationScoped
public class CamundaWorkflowNotifier implements WorkflowNotifier {

    private static final Logger LOG = Logger.getLogger(CamundaWorkflowNotifier.class);

    private final CamundaMessageClient client;
    private final ObjectMapper objectMapper;
    private final int maxAttempts;
    private final long waitMs;

    public CamundaWorkflowNotifier(@RestClient CamundaMessageClient client,
                                   ObjectMapper objectMapper,
                                   @ConfigProperty(name = "ai.processengine.correlation.max-attempts", defaultValue = "5")
                                   int maxAttempts,
                                   @ConfigProperty(name = "ai.processengine.correlation.wait-ms", defaultValue = "1000")
                                   long waitMs) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.waitMs = waitMs;
    }

    @Override
    public void sendeErgebnis(String businessKey, ErgebnisKi ergebnis) {
        CamundaCorrelationRequest request = CamundaCorrelationRequest.ergebnisKI(businessKey, toJson(ergebnis));

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            int status = correlate(request);
            if (status < 400) {
                LOG.infof("ergebnisKI an Process Engine korreliert (businessKey=%s, HTTP %d).", businessKey, status);
                return;
            }
            if (status == 400 && attempt < maxAttempts) {
                LOG.infof("Process Engine wartet noch nicht auf ergebnisKI (businessKey=%s, Versuch %d/%d).",
                        businessKey, attempt, maxAttempts);
                pause();
                continue;
            }
            throw new IllegalStateException("Process Engine hat ergebnisKI abgelehnt: HTTP " + status
                    + " (businessKey=" + businessKey + ")");
        }
    }

    private int correlate(CamundaCorrelationRequest request) {
        try (Response response = client.correlate(request)) {
            return response.getStatus();
        } catch (WebApplicationException e) {
            return e.getResponse() != null ? e.getResponse().getStatus() : 500;
        } catch (ProcessingException e) {
            throw new IllegalStateException("Process Engine nicht erreichbar (CAMUNDA_ENGINE_URL pruefen): "
                    + e.getMessage(), e);
        }
    }

    private String toJson(ErgebnisKi ergebnis) {
        try {
            return objectMapper.writeValueAsString(ergebnis);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("ergebnisKI konnte nicht serialisiert werden.", e);
        }
    }

    private void pause() {
        try {
            Thread.sleep(waitMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Unterbrochen beim Warten auf die Process Engine.", e);
        }
    }
}
