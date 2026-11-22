package de.werkmundo.invoice.infrastructure.offer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OfferPositionResponse(
        String hersteller,
        String bezeichnung,
        BigDecimal menge,
        String einheit,
        String katalogProduktId,
        BigDecimal einzelPreis,
        Integer reihenfolge,
        String type
) {
}