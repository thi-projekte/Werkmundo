package de.werkmundo.offer.application;

import de.werkmundo.offer.application.security.OfferAccessTokenService;
import de.werkmundo.offer.application.usecase.customer.GetOfferByAccessTokenUseCase;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.werkmundo.offer.domain.Offer;
import de.werkmundo.offer.domain.OfferRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetOfferByAccessTokenUseCaseTest {

    @Test
    void returnsOfferWithValidCustomerToken() {
        OfferAccessTokenService tokenService = new OfferAccessTokenService();
        Offer offer = new Offer("offer-123");
        String token = OfferAccessTokenTestSupport.issueForSentOffer(offer, tokenService);
        GetOfferByAccessTokenUseCase useCase = new GetOfferByAccessTokenUseCase(
                repositoryFor(offer), tokenService, OfferAccessTokenTestSupport.CLOCK);

        assertSame(offer, useCase.execute("offer-123", token));
    }

    @Test
    void rejectsInvalidAndExpiredTokens() {
        OfferAccessTokenService tokenService = new OfferAccessTokenService();
        Offer offer = new Offer("offer-123");
        String token = OfferAccessTokenTestSupport.issueForSentOffer(offer, tokenService);
        Clock expiredClock = Clock.fixed(
                Instant.parse("2026-11-15T12:00:00Z").plusSeconds(1),
                ZoneOffset.UTC);
        GetOfferByAccessTokenUseCase useCase = new GetOfferByAccessTokenUseCase(
                repositoryFor(offer), tokenService, expiredClock);

        assertThrows(SecurityException.class, () -> useCase.execute("offer-123", "wrong-token"));
        assertThrows(SecurityException.class, () -> useCase.execute("offer-123", token));
    }

    @Test
    void tokenRemainsValidForViewingAfterCustomerRequestsChanges() {
        OfferAccessTokenService tokenService = new OfferAccessTokenService();
        Offer offer = new Offer("offer-123");
        String token = OfferAccessTokenTestSupport.issueForSentOffer(offer, tokenService);
        offer.requestChanges("Please update the delivery date");
        GetOfferByAccessTokenUseCase useCase = new GetOfferByAccessTokenUseCase(
                repositoryFor(offer), tokenService, OfferAccessTokenTestSupport.CLOCK);

        assertSame(offer, useCase.execute("offer-123", token));
    }

    @Test
    void throwsWhenOfferDoesNotExist() {
        GetOfferByAccessTokenUseCase useCase = new GetOfferByAccessTokenUseCase(repositoryFor(null));

        assertThrows(NoSuchElementException.class, () -> useCase.execute("missing-offer", "token"));
    }

    private OfferRepository repositoryFor(Offer offer) {
        return new OfferRepository() {
            @Override
            public Offer save(Offer offerToSave) {
                return offerToSave;
            }

            @Override
            public Optional<Offer> findByBusinessKey(String businessKey) {
                return offer != null && businessKey.equals(offer.getBusinessKey())
                        ? Optional.of(offer)
                        : Optional.empty();
            }

            @Override
            public List<Offer> findAllByCraftsmanId(String craftsmanId) {
                return List.of();
            }
        };
    }
}
