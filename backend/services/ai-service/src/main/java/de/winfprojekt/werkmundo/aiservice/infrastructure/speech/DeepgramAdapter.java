package de.winfprojekt.werkmundo.aiservice.infrastructure.speech;

import de.winfprojekt.werkmundo.aiservice.application.TranskriptionException;
import de.winfprojekt.werkmundo.aiservice.application.port.SpeechToTextPort;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

/**
 * {@link SpeechToTextPort}-Implementierung fuer Deepgram ({@code POST /v1/listen}).
 * Uebernommen aus dem offer-service des Vorsemesters, da die Sprachaufnahme laut
 * Team-Ownership zum ai-service (Squad P) gehoert.
 */
@ApplicationScoped
public class DeepgramAdapter implements SpeechToTextPort {

    private static final int MAX_ERROR_CHARS = 300;

    private final Optional<String> apiKey;
    private final String url;
    private final String model;
    private final String language;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public DeepgramAdapter(@ConfigProperty(name = "deepgram.api.key") Optional<String> apiKey,
                           @ConfigProperty(name = "deepgram.api.url") String url,
                           @ConfigProperty(name = "deepgram.model") String model,
                           @ConfigProperty(name = "deepgram.language") String language,
                           ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.url = url;
        this.model = model;
        this.language = language;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean isConfigured() {
        return apiKey.filter(k -> !k.isBlank() && !"changeme".equals(k)).isPresent();
    }

    @Override
    public String transcribe(byte[] audio, String contentType) {
        if (!isConfigured()) {
            throw new TranskriptionException("DEEPGRAM_API_KEY ist nicht gesetzt.");
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url + "?language=" + language + "&model=" + model + "&smart_format=true"))
                .header("Authorization", "Token " + apiKey.get())
                .header("Content-Type", contentType == null || contentType.isBlank() ? "audio/webm" : contentType)
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofByteArray(audio))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new TranskriptionException("Deepgram antwortet mit HTTP " + response.statusCode()
                        + ": " + kuerzen(response.body()));
            }
            return parseTranscript(response.body());
        } catch (IOException e) {
            throw new TranskriptionException("Deepgram nicht erreichbar: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TranskriptionException("Transkription wurde unterbrochen.", e);
        }
    }

    /** Liest {@code results.channels[0].alternatives[0].transcript}; leerer String, wenn nicht vorhanden. */
    String parseTranscript(String json) {
        try {
            JsonNode transcript = objectMapper.readTree(json)
                    .path("results").path("channels").path(0)
                    .path("alternatives").path(0).path("transcript");
            return transcript.isTextual() ? transcript.asText().trim() : "";
        } catch (IOException e) {
            throw new TranskriptionException("Deepgram-Antwort nicht lesbar: " + e.getMessage(), e);
        }
    }

    private static String kuerzen(String body) {
        if (body == null) {
            return "";
        }
        return body.length() <= MAX_ERROR_CHARS ? body : body.substring(0, MAX_ERROR_CHARS) + "...";
    }
}
