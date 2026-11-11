package de.winfprojekt.werkmundo.aiservice.application;

import de.winfprojekt.werkmundo.aiservice.application.port.SpeechToTextPort;
import de.winfprojekt.werkmundo.aiservice.domain.LeeresTranskriptException;
import de.winfprojekt.werkmundo.aiservice.domain.UngueltigerAuftragException;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

/**
 * Sprachaufnahme des Handwerkers → Text (Sprachschnipsel).
 *
 * <p>Das Transkript geht zurueck an das Frontend. Von dort wird es als {@code speechSnippet}
 * an den offer-service uebergeben, der den Prozess startet; die Process Engine ruft danach
 * {@code POST /ai/process} auf (LLM-Call 1 und 2).
 *
 * <p>Ohne {@code DEEPGRAM_API_KEY} liefert der Use-Case ein festes Beispiel-Transkript, damit
 * der Ablauf lokal ohne Keys getestet werden kann (gleiches Prinzip wie der LLM-Stub).
 */
@ApplicationScoped
public class TranscribeAudioUseCase {

    private static final Logger LOG = Logger.getLogger(TranscribeAudioUseCase.class);

    static final String STUB_TRANSKRIPT =
            "Beim Kunden soll in der Garage eine Wallbox installiert werden. "
                    + "Dafür brauchen wir etwa zehn Meter Starkstromkabel und einen FI-Schutzschalter.";

    private final SpeechToTextPort speechToText;

    public TranscribeAudioUseCase(SpeechToTextPort speechToText) {
        this.speechToText = speechToText;
    }

    public String transkribiere(byte[] audio, String contentType) {
        if (audio == null || audio.length == 0) {
            throw new UngueltigerAuftragException("Die Audioaufnahme ist leer.");
        }
        if (contentType != null && !contentType.isBlank()
                && !contentType.startsWith("audio/") && !contentType.startsWith("video/webm")) {
            throw new UngueltigerAuftragException("Nur Audiodateien werden akzeptiert (erhalten: " + contentType + ").");
        }

        if (!speechToText.isConfigured()) {
            LOG.warn("DEEPGRAM_API_KEY ist nicht gesetzt - liefere Beispiel-Transkript (Stub).");
            return STUB_TRANSKRIPT;
        }

        String transkript = speechToText.transcribe(audio, contentType);
        if (transkript == null || transkript.isBlank()) {
            throw new LeeresTranskriptException();
        }
        return transkript.trim();
    }
}
