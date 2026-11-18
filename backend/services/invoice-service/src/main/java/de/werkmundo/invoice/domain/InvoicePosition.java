package de.werkmundo.invoice.domain;

import java.math.BigDecimal;

/**
 * Eigenständige Kopie einer Position für eine Rechnung.
 * Die Katalogprodukt-ID ist eine logische Referenz.
 */
public record InvoicePosition(
        String hersteller,
        String bezeichnung,
        BigDecimal menge,
        String einheit,
        String katalogProduktId,
        BigDecimal einzelPreis,
        BigDecimal positionsPreis,
        int reihenfolge,
        InvoicePositionType type
) {
}