package de.werkmundo.offer.api.dto;

import de.werkmundo.offer.application.command.UpdateOfferCommand;
import jakarta.ws.rs.BadRequestException;
import java.math.BigDecimal;
import java.util.List;

public record UpdateOfferRequest(
        String customerId,
        BigDecimal estimatedWorkDurationHours,
        List<String> correctionSuggestions,
        List<OfferPositionRequest> positions) {

    public UpdateOfferCommand toCommand(String businessKey, String craftsmanId) {
        if (businessKey == null || businessKey.isBlank()) {
            throw new BadRequestException("businessKey is required");
        }
        if (correctionSuggestions == null) {
            throw new BadRequestException("correctionSuggestions is required");
        }
        return new UpdateOfferCommand(
                businessKey,
                customerId,
                craftsmanId,
                estimatedWorkDurationHours,
                correctionSuggestions,
                CreateOfferRequest.requirePositions(positions));
    }
}
