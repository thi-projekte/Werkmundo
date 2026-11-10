package de.werkmundo.offer.application;

import de.werkmundo.offer.application.security.GeneratedOfferAccessToken;
import de.werkmundo.offer.application.security.OfferAccessTokenService;
import de.werkmundo.offer.domain.Offer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

final class OfferAccessTokenTestSupport {
    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-08T12:00:00Z"), ZoneOffset.UTC);

    private OfferAccessTokenTestSupport() {
    }

    static String issueForSentOffer(Offer offer, OfferAccessTokenService tokenService) {
        offer.markReadyToSend();
        GeneratedOfferAccessToken token = tokenService.generate();
        offer.setCustomerAccessToken(token.tokenHash(), CLOCK.instant().plus(Duration.ofDays(7)));
        offer.markSent();
        return token.token();
    }
}
