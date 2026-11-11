package de.winfprojekt.werkmundo.aiservice.application;

/**
 * Die Spracherkennung ist fehlgeschlagen (Anbieter nicht erreichbar, HTTP-Fehler, unlesbare
 * Antwort). Wird im api-Layer auf HTTP 502 abgebildet.
 */
public class TranskriptionException extends RuntimeException {

    public TranskriptionException(String message) {
        super(message);
    }

    public TranskriptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
