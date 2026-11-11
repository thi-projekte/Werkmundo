package de.winfprojekt.werkmundo.aiservice.domain;

/** In der Aufnahme wurde keine Sprache erkannt. Wird im api-Layer auf HTTP 422 abgebildet. */
public class LeeresTranskriptException extends RuntimeException {

    public LeeresTranskriptException() {
        super("In der Aufnahme wurde keine Sprache erkannt. Bitte erneut aufnehmen.");
    }
}
