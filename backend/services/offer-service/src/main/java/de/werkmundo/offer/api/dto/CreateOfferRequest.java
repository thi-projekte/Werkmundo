package de.werkmundo.offer.api.dto;

import de.werkmundo.offer.application.command.CreateOfferCommand;
import jakarta.ws.rs.BadRequestException;
import java.math.BigDecimal;
import java.util.List;

public record CreateOfferRequest(
        String businessKey,
        String customerId,
        BigDecimal estimatedWorkDurationHours,
        List<String> correctionSuggestions,
        List<OfferPositionRequest> positions) {

    public CreateOfferCommand toCommand(String craftsmanId) {
        if (businessKey == null || businessKey.isBlank()) {
            throw new BadRequestException("businessKey is required");
        }
        return new CreateOfferCommand(
                businessKey,
                customerId,
                craftsmanId,
                estimatedWorkDurationHours,
                requireList(correctionSuggestions, "correctionSuggestions"),
                requirePositions(positions));
    }

    static List<de.werkmundo.offer.application.command.CreateOfferPositionCommand> requirePositions(
            List<OfferPositionRequest> positions) {
        if (positions == null) {
            throw new BadRequestException("positions is required");
        }
        return positions.stream()
                .map(position -> {
                    if (position == null) {
                        throw new BadRequestException("positions must not contain null entries");
                    }
                    return position.toCommand();
                })
                .toList();
    }

    private static <T> List<T> requireList(List<T> values, String name) {
        if (values == null) {
            throw new BadRequestException(name + " is required");
        }
        return values;
    }
}
