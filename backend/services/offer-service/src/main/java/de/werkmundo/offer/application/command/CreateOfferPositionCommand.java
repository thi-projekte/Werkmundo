package de.werkmundo.offer.application.command;

import de.werkmundo.offer.domain.OfferPositionType;
import java.math.BigDecimal;

public record CreateOfferPositionCommand(
        OfferPositionType type,
        String name,
        String description,
        String manufacturer,
        BigDecimal quantity,
        String unit,
        String catalogProductId,
        BigDecimal unitPrice,
        int positionOrder) {
}
