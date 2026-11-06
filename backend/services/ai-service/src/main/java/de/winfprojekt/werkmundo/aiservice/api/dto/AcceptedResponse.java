package de.winfprojekt.werkmundo.aiservice.api.dto;

/**
 * Sofortige Antwort (HTTP 202) auf {@code POST /ai/process}. Das eigentliche Ergebnis geht
 * spaeter asynchron als Message {@code ergebnisKI} an die Process Engine.
 */
public record AcceptedResponse(String status, String businessKey) {

    public static AcceptedResponse accepted(String businessKey) {
        return new AcceptedResponse("accepted", businessKey);
    }
}
