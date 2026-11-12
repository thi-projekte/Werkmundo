package de.werkmundo.offer.api.dto;

import de.werkmundo.offer.domain.Offer;
import de.werkmundo.offer.domain.OfferPosition;
import de.werkmundo.offer.domain.OfferPositionType;
import de.werkmundo.offer.domain.OfferStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OfferResponse(
        String businessKey,
        OfferStatus status,
        BigDecimal estimatedWorkDurationHours,
        List<String> correctionSuggestions,
        List<String> customerChangeRequests,
        BigDecimal totalPrice,
        List<PositionResponse> positions,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static OfferResponse from(Offer offer) {
        return new OfferResponse(
                offer.getBusinessKey(),
                offer.getStatus(),
                offer.getEstimatedWorkDurationHours(),
                offer.getCorrectionSuggestions(),
                offer.getCustomerChangeRequests(),
                offer.getTotalPrice(),
                offer.getPositions().stream().map(PositionResponse::from).toList(),
                offer.getCreatedAt(),
                offer.getUpdatedAt());
    }

    public record PositionResponse(
            OfferPositionType type,
            String name,
            String description,
            String manufacturer,
            BigDecimal quantity,
            String unit,
            String catalogProductId,
            BigDecimal unitPrice,
            BigDecimal lineTotal,
            int positionOrder) {

        private static PositionResponse from(OfferPosition position) {
            return new PositionResponse(
                    position.getType(),
                    position.getName(),
                    position.getDescription(),
                    position.getManufacturer(),
                    position.getQuantity(),
                    position.getUnit(),
                    position.getCatalogProductId(),
                    position.getUnitPrice(),
                    position.getLineTotal(),
                    position.getPositionOrder());
        }
    }
}
