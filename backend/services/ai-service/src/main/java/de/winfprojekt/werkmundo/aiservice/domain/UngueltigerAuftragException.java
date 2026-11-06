package de.winfprojekt.werkmundo.aiservice.domain;

/**
 * Der Eingangs-Payload laesst sich weder als Erstangebot noch als Korrektur zuordnen.
 * Die api-Schicht beantwortet das mit HTTP 400.
 */
public class UngueltigerAuftragException extends IllegalArgumentException {

    public UngueltigerAuftragException(String message) {
        super(message);
    }
}
