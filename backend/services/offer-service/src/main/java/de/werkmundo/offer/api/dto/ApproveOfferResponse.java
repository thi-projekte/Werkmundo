package de.werkmundo.offer.api.dto;

import de.werkmundo.offer.application.usecase.offer.ApproveOfferResult;
import java.time.Instant;

public record ApproveOfferResponse(
        OfferResponse offer,
        String accessToken,
        Instant accessTokenExpiresAt) {

    public static ApproveOfferResponse from(ApproveOfferResult result) {
        return new ApproveOfferResponse(
                OfferResponse.from(result.offer()),
                result.accessToken(),
                result.accessTokenExpiresAt());
    }
}
