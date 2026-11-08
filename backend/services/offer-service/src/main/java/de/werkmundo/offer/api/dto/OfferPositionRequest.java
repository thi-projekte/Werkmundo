package de.werkmundo.offer.api.dto;

import de.werkmundo.offer.application.command.CreateOfferPositionCommand;
import de.werkmundo.offer.domain.OfferPositionType;
import jakarta.ws.rs.BadRequestException;
import java.math.BigDecimal;

public record OfferPositionRequest(
        OfferPositionType type,
        String name,
        String description,
        String manufacturer,
        BigDecimal quantity,
        String unit,
        String catalogProductId,
        BigDecimal unitPrice,
        int positionOrder) {

    public CreateOfferPositionCommand toCommand() {
        if (type == null || name == null || name.isBlank() || quantity == null || unitPrice == null) {
            throw new BadRequestException("Each position requires a type, name, quantity, and unitPrice");
        }
        return new CreateOfferPositionCommand(
                type, name, description, manufacturer, quantity, unit, catalogProductId, unitPrice, positionOrder);
    }
}
