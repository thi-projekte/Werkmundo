package de.winfprojekt.werkmundo.aiservice.api;

import de.winfprojekt.werkmundo.aiservice.api.dto.TranskriptResponse;
import de.winfprojekt.werkmundo.aiservice.application.TranscribeAudioUseCase;
import de.winfprojekt.werkmundo.aiservice.domain.UngueltigerAuftragException;

import io.smallrye.common.annotation.Blocking;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Arrays;

/**
 * Spracheingabe aus dem Frontend: {@code POST /ai/transcribe} (multipart, Feld {@code audio})
 * → {@code {"transkript": "..."}}.
 *
 * <p>Zugriffsschutz per Konfiguration ({@code AI_TRANSCRIBE_AUTH_POLICY}): lokal offen,
 * online {@code authenticated} (Keycloak-Token des eingeloggten Handwerkers).
 * Die Aufnahme wird nicht gespeichert: Temp-Datei und Bytes werden direkt verworfen.
 */
@Path("/ai/transcribe")
public class TranscriptionResource {

    private final TranscribeAudioUseCase useCase;

    public TranscriptionResource(TranscribeAudioUseCase useCase) {
        this.useCase = useCase;
    }

    @POST
    @Blocking
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public TranskriptResponse transcribe(@RestForm("audio") FileUpload audio) {
        if (audio == null || audio.uploadedFile() == null) {
            throw new UngueltigerAuftragException("Feld 'audio' fehlt.");
        }
        byte[] daten = null;
        try {
            daten = Files.readAllBytes(audio.uploadedFile());
            return new TranskriptResponse(useCase.transkribiere(daten, audio.contentType()));
        } catch (IOException e) {
            throw new UncheckedIOException("Audiodatei konnte nicht gelesen werden.", e);
        } finally {
            if (daten != null) {
                Arrays.fill(daten, (byte) 0);
            }
            try {
                Files.deleteIfExists(audio.uploadedFile());
            } catch (IOException ignored) {
                // Temp-Datei raeumt Quarkus spaetestens beim Request-Ende selbst weg.
            }
        }
    }
}
