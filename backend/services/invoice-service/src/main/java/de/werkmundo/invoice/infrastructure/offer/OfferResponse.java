package de.werkmundo.invoice.infrastructure.offer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OfferResponse(
        String businessKey,
        String status,
        String customerId,
        String handwerkerId,
        List<OfferPositionResponse> positions
) {
}