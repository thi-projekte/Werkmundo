package de.werkmundo.invoice.domain;

/**
 * Kundendaten zum Zeitpunkt der Rechnungserstellung.
 * Spätere Änderungen am Kundenprofil verändern diesen Teil nicht.
 */
public record InvoiceCustomerSnapshot(
        String vorname,
        String nachname,
        String email,
        String strasse,
        String hausnummer,
        String plz,
        String ort
) {
}