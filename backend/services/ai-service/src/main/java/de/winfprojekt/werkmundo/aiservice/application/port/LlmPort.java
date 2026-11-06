package de.winfprojekt.werkmundo.aiservice.application.port;

import de.winfprojekt.werkmundo.aiservice.application.LlmException;

/**
 * Anbieterneutraler Zugriff auf ein Chat-LLM. Modellwahl, Retries und Modell-Fallback sind
 * Sache der Implementierung.
 */
public interface LlmPort {

    /** {@code true}, wenn das LLM nutzbar ist (z.B. API-Key gesetzt). Sonst nutzt die Pipeline den Fallback. */
    boolean isConfigured();

    /**
     * Fuehrt einen Chat-Completion-Aufruf aus und liefert den rohen Textinhalt der Antwort.
     *
     * @throws LlmException bei endgueltigem Misserfolg
     */
    String complete(String systemPrompt, String userContent);
}
