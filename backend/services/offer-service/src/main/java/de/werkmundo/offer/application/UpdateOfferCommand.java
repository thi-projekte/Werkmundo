package de.werkmundo.offer.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record UpdateOfferCommand(
        String businessKey,
        String customerId,
        String craftsmanId,
        String acceptanceToken,
        BigDecimal estimatedWorkDurationHours,
        List<String> correctionSuggestions,
        List<CreateOfferPositionCommand> positions) {

    public UpdateOfferCommand {
        Objects.requireNonNull(businessKey, "businessKey must not be null");
        correctionSuggestions = List.copyOf(
                Objects.requireNonNull(correctionSuggestions, "correctionSuggestions must not be null"));
        positions = List.copyOf(Objects.requireNonNull(positions, "positions must not be null"));
    }
}
