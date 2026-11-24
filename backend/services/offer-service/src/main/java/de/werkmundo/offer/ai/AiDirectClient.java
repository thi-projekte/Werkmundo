package de.werkmundo.offer.ai;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.Optional;

/**
 * Direkter Aufruf des ai-service ohne Process Engine (Übergangslösung für den Online-Betrieb).
 *
 * <p>Ein Fehler oder Timeout der KI darf das Anlegen des Angebots nicht verhindern: dann kommt
 * {@link Optional#empty()} zurück und der Handwerker füllt die Positionen selbst aus.
 */
@ApplicationScoped
public class AiDirectClient {

    private static final Logger LOG = Logger.getLogger(AiDirectClient.class);

    @ConfigProperty(name = "ai.direct.enabled", defaultValue = "false")
    boolean enabled;

    @Inject
    @RestClient
    AiServiceRestClient client;

    public boolean isEnabled() {
        return enabled;
    }

    public Optional<AiAnalyzeResult> analysiere(String businessKey, String handwerkerId, String sprachschnipsel) {
        if (sprachschnipsel == null || sprachschnipsel.isBlank()) {
            return Optional.empty();
        }
        long start = System.currentTimeMillis();
        try {
            AiAnalyzeResult result = client.analyze(
                    AiAnalyzeRequest.erstangebot(businessKey, sprachschnipsel), handwerkerId);
            LOG.infof("KI-Ergebnis für %s nach %d ms erhalten", businessKey, System.currentTimeMillis() - start);
            return Optional.ofNullable(result);
        } catch (Exception e) {
            LOG.errorf(e, "ai-service für %s nicht erfolgreich (nach %d ms), Angebot wird ohne KI-Positionen angelegt",
                    businessKey, System.currentTimeMillis() - start);
            return Optional.empty();
        }
    }
}
