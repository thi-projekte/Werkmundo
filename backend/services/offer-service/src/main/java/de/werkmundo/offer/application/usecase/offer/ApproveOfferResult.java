package de.werkmundo.offer.application.usecase.offer;

import de.werkmundo.offer.domain.Offer;
import java.time.Instant;

public final class ApproveOfferResult {
    private final Offer offer;
    private final String accessToken;
    private final Instant accessTokenExpiresAt;

    ApproveOfferResult(Offer offer, String accessToken, Instant accessTokenExpiresAt) {
        this.offer = offer;
        this.accessToken = accessToken;
        this.accessTokenExpiresAt = accessTokenExpiresAt;
    }

    public Offer offer() {
        return offer;
    }

    public String accessToken() {
        return accessToken;
    }

    public Instant accessTokenExpiresAt() {
        return accessTokenExpiresAt;
    }

    @Override
    public String toString() {
        return "ApproveOfferResult[offer=" + offer.getBusinessKey() + ", accessToken=redacted, "
                + "accessTokenExpiresAt=" + accessTokenExpiresAt + "]";
    }
}
