package de.werkmundo.invoice.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Rechnungsdaten, unabhängig von Datenbank und PDF-Erzeugung.
 */
public record Invoice(
        UUID id,
        String ownerId,
        String offerBusinessKey,
        String rechnungsnummer,
        InvoiceCustomerSnapshot kundendaten,
        BigDecimal gesamtPreis,
        Instant createdAt,
        Instant updatedAt,
        List<InvoicePosition> positions
) {

    public Invoice {
        Objects.requireNonNull(id, "id darf nicht fehlen");
        Objects.requireNonNull(ownerId, "ownerId darf nicht fehlen");
        Objects.requireNonNull(offerBusinessKey, "offerBusinessKey darf nicht fehlen");
        Objects.requireNonNull(kundendaten, "kundendaten dürfen nicht fehlen");
        Objects.requireNonNull(gesamtPreis, "gesamtPreis darf nicht fehlen");
        Objects.requireNonNull(createdAt, "createdAt darf nicht fehlen");
        Objects.requireNonNull(updatedAt, "updatedAt darf nicht fehlen");

        positions = List.copyOf(positions);
    }
}