package de.winfprojekt.werkmundo.aiservice.infrastructure.llm;

import de.winfprojekt.werkmundo.aiservice.application.LlmException;
import de.winfprojekt.werkmundo.aiservice.application.port.LlmPort;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Optional;

/**
 * {@link LlmPort}-Implementierung fuer MegaLLM.
 *
 * <ul>
 *   <li>HTTP 429/5xx und Timeouts → bis zu {@value #MAX_RETRIES} Retries mit Backoff (1s, 2s, 4s).</li>
 *   <li><b>Modell-Fallback:</b> scheitert das Primaermodell endgueltig (z.B. 503 "Resource overloaded"
 *       fuer die ganze Gemini-Familie), wird einmal das Fallback-Modell versucht.</li>
 *   <li>Leere Antwort → {@link LlmException}.</li>
 * </ul>
 *
 * <p>Blockiert den aufrufenden Thread (Backoff per {@link Thread#sleep}) - gewollt, da der Aufruf
 * im Hintergrund-Teil des Use-Cases laeuft.
 */
@ApplicationScoped
public class MegaLlmAdapter implements LlmPort {

    private static final Logger LOG = Logger.getLogger(MegaLlmAdapter.class);
    static final int MAX_RETRIES = 3;
    private static final double TEMPERATURE = 0.2;
    private static final int MAX_ERROR_CHARS = 300;

    private final MegaLlmClient client;
    private final Optional<String> apiKey;
    private final String model;
    private final Optional<String> fallbackModel;
    private final long backoffBaseMs;

    public MegaLlmAdapter(@RestClient MegaLlmClient client,
                          @ConfigProperty(name = "megallm.api.key") Optional<String> apiKey,
                          @ConfigProperty(name = "megallm.model.default") String model,
                          @ConfigProperty(name = "megallm.model.fallback") Optional<String> fallbackModel,
                          @ConfigProperty(name = "megallm.retry.backoff-ms", defaultValue = "1000") long backoffBaseMs) {
        this.client = client;
        this.apiKey = apiKey;
        this.model = model;
        this.fallbackModel = fallbackModel;
        this.backoffBaseMs = backoffBaseMs;
    }

    @Override
    public boolean isConfigured() {
        return apiKey.filter(k -> !k.isBlank()).isPresent();
    }

    @Override
    public String complete(String systemPrompt, String userContent) {
        if (!isConfigured()) {
            throw new LlmException("MEGALLM_API_KEY ist nicht gesetzt.");
        }
        try {
            return completeWith(model, systemPrompt, userContent);
        } catch (RuntimeException primaryFailure) {
            String fallback = fallbackModel.filter(m -> !m.isBlank()).orElse(null);
            if (fallback == null || fallback.equals(model)) {
                throw primaryFailure;
            }
            LOG.warnf("Primaermodell %s fehlgeschlagen (%s) - Fallback auf %s.",
                    model, primaryFailure.getMessage(), fallback);
            return completeWith(fallback, systemPrompt, userContent);
        }
    }

    /** Ein Aufruf gegen genau ein Modell, inkl. Retry bei transienten Fehlern. */
    private String completeWith(String modelId, String systemPrompt, String userContent) {
        ChatRequest request = new ChatRequest(modelId,
                List.of(new ChatRequest.Message("system", systemPrompt),
                        new ChatRequest.Message("user", userContent)),
                TEMPERATURE);

        for (int attempt = 1; ; attempt++) {
            int status;
            String error;
            try (Response response = client.complete("Bearer " + apiKey.orElseThrow(), request)) {
                status = response.getStatus();
                if (status >= 200 && status < 300) {
                    ChatResponse body = response.readEntity(ChatResponse.class);
                    String content = body != null ? body.firstContent() : null;
                    if (content == null || content.isBlank()) {
                        throw new LlmException("MegaLLM lieferte eine leere Antwort (" + modelId + ").");
                    }
                    return content;
                }
                error = readError(response);
            } catch (WebApplicationException e) {
                // Der REST-Client wirft bei Status >= 400 je nach Konfiguration schon selbst.
                status = e.getResponse() != null ? e.getResponse().getStatus() : 500;
                error = e.getMessage();
            } catch (ProcessingException e) {
                if (attempt <= MAX_RETRIES) {
                    LOG.warnf("MegaLLM nicht erreichbar (Versuch %d/%d): %s", attempt, MAX_RETRIES + 1, e.getMessage());
                    backoff(attempt);
                    continue;
                }
                throw new LlmException("MegaLLM nicht erreichbar (Timeout/Netzwerk): " + e.getMessage(), e);
            }

            if (isTransient(status) && attempt <= MAX_RETRIES) {
                LOG.warnf("MegaLLM HTTP %d (Versuch %d/%d) - neuer Versuch.", status, attempt, MAX_RETRIES + 1);
                backoff(attempt);
                continue;
            }
            throw new LlmException("MegaLLM HTTP " + status + " (" + modelId + "): " + error);
        }
    }

    private static boolean isTransient(int status) {
        return status == 429 || status >= 500;
    }

    private void backoff(int attempt) {
        try {
            Thread.sleep(backoffBaseMs * (1L << (attempt - 1)));
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new LlmException("Waehrend des Backoffs unterbrochen.", ie);
        }
    }

    private static String readError(Response response) {
        try {
            String body = response.readEntity(String.class);
            if (body == null) {
                return "";
            }
            return body.length() > MAX_ERROR_CHARS ? body.substring(0, MAX_ERROR_CHARS) : body;
        } catch (RuntimeException e) {
            return "<Fehlertext nicht lesbar>";
        }
    }
}
