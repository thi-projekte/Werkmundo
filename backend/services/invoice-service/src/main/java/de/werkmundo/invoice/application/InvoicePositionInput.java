package de.werkmundo.invoice.application;

import de.werkmundo.invoice.domain.InvoicePositionType;
import java.math.BigDecimal;

/**
 * Eingabedaten für eine Rechnungsposition.
 * Positionssumme und Reihenfolge werden vom Server bestimmt.
 */
public record InvoicePositionInput(
        String hersteller,
        String bezeichnung,
        BigDecimal menge,
        String einheit,
        String katalogProduktId,
        BigDecimal einzelPreis,
        InvoicePositionType type
) {
}