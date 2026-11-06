package de.winfprojekt.werkmundo.aiservice.application;

/**
 * Ein LLM-Aufruf ist fehlgeschlagen (HTTP-Fehler nach Retries, Timeout, leere oder unparsbare
 * Antwort). Die Pipeline faellt dann auf den Stub bzw. die Baseline zurueck.
 */
public class LlmException extends RuntimeException {

    public LlmException(String message) {
        super(message);
    }

    public LlmException(String message, Throwable cause) {
        super(message, cause);
    }
}
