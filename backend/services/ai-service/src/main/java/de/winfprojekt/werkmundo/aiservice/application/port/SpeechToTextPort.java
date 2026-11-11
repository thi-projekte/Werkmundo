package de.winfprojekt.werkmundo.aiservice.application.port;

import de.winfprojekt.werkmundo.aiservice.application.TranskriptionException;

/**
 * Anbieterneutrale Spracherkennung (Speech-to-Text). Die konkrete Anbindung (z.B. Deepgram)
 * liegt in der infrastructure-Schicht.
 */
public interface SpeechToTextPort {

    /** {@code true}, wenn der Anbieter nutzbar ist (z.B. API-Key gesetzt). Sonst nutzt der Use-Case den Stub. */
    boolean isConfigured();

    /**
     * Wandelt eine Audioaufnahme in Text um.
     *
     * @param audio       Rohdaten der Aufnahme
     * @param contentType MIME-Typ, z.B. {@code audio/webm}
     * @return erkannter Text (kann leer sein, wenn nichts verstanden wurde)
     * @throws TranskriptionException wenn der Anbieter nicht erreichbar ist oder einen Fehler liefert
     */
    String transcribe(byte[] audio, String contentType);
}
