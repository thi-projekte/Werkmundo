package de.werkmundo.offer.application;

import de.werkmundo.offer.application.security.OfferAccessTokenService;
import de.werkmundo.offer.application.usecase.offer.ApproveOfferResult;
import de.werkmundo.offer.application.usecase.offer.ApproveOfferUseCase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.werkmundo.offer.domain.Offer;
import de.werkmundo.offer.domain.OfferRepository;
import de.werkmundo.offer.domain.OfferStatus;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ApproveOfferUseCaseTest {

    @Test
    void approvesDraftOfferForSending() {
        Offer offer = new Offer("offer-123");
        AtomicInteger saveCalls = new AtomicInteger();
        OfferAccessTokenService tokenService = new OfferAccessTokenService();
        ApproveOfferUseCase useCase = new ApproveOfferUseCase(
                repositoryFor(offer, saveCalls),
                Duration.ofDays(3),
                java.time.Clock.fixed(Instant.parse("2026-11-08T12:00:00Z"), ZoneOffset.UTC),
                tokenService);

        ApproveOfferResult result = useCase.execute("offer-123");

        assertSame(offer, result.offer());
        assertEquals(OfferStatus.READY_TO_SEND, result.offer().getStatus());
        assertEquals(2, result.offer().getStatusHistory().size());
        assertEquals(Instant.parse("2026-11-11T12:00:00Z"), result.accessTokenExpiresAt());
        assertFalse(result.accessToken().isBlank());
        assertNotEquals(result.accessToken(), result.offer().customerAccessTokenHash());
        assertFalse(result.toString().contains(result.accessToken()));
        assertThrows(SecurityException.class,
                () -> tokenService.requireValid(result.offer(), result.accessToken(),
                        Instant.parse("2026-11-08T12:00:00Z")));
        assertEquals(1, saveCalls.get());
    }

    @Test
    void rejectsApprovalWhenOfferIsNotDraft() {
        Offer offer = new Offer("offer-123");
        offer.markReadyToSend();
        AtomicInteger saveCalls = new AtomicInteger();
        ApproveOfferUseCase useCase = new ApproveOfferUseCase(repositoryFor(offer, saveCalls));

        assertThrows(IllegalStateException.class, () -> useCase.execute("offer-123"));

        assertEquals(OfferStatus.READY_TO_SEND, offer.getStatus());
        assertNull(offer.customerAccessTokenHash());
        assertEquals(0, saveCalls.get());
    }

    @Test
    void throwsWhenOfferDoesNotExist() {
        ApproveOfferUseCase useCase = new ApproveOfferUseCase(repositoryFor(null, new AtomicInteger()));

        assertThrows(NoSuchElementException.class, () -> useCase.execute("missing-offer"));
    }

    @Test
    void rejectsNonPositiveTokenLifetime() {
        assertThrows(IllegalArgumentException.class,
                () -> new ApproveOfferUseCase(repositoryFor(null, new AtomicInteger()), Duration.ZERO));
    }

    private OfferRepository repositoryFor(Offer offer, AtomicInteger saveCalls) {
        return new OfferRepository() {
            @Override
            public Offer save(Offer offerToSave) {
                saveCalls.incrementAndGet();
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
