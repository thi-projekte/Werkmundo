package de.werkmundo.offer.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.werkmundo.offer.domain.Offer;
import de.werkmundo.offer.domain.OfferRepository;
import de.werkmundo.offer.domain.OfferStatus;
import de.werkmundo.offer.domain.OfferPositionType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class UpdateOfferUseCaseTest {

    @Test
    void updatesOfferAfterCustomerChangeRequestAndReturnsItToDraft() {
        Offer existingOffer = new Offer("business-123");
        existingOffer.markReadyToSend();
        existingOffer.markSent();
        existingOffer.requestChanges("Please update the offer");
        AtomicInteger saveCalls = new AtomicInteger();
        UpdateOfferUseCase useCase = new UpdateOfferUseCase(repositoryFor(existingOffer, saveCalls));

        Offer result = useCase.execute(updateCommand("business-123"));

        assertSame(existingOffer, result);
        assertEquals("customer-456", result.getCustomerId());
        assertEquals("craftsman-789", result.getCraftsmanId());
        assertEquals(new BigDecimal("25.00"), result.getTotalPrice());
        assertEquals(OfferStatus.DRAFT, result.getStatus());
        assertEquals(5, result.getStatusHistory().size());
        assertEquals(1, result.getPositions().size());
        assertSame(result, result.getPositions().get(0).getOffer());
        assertEquals("Wood", result.getPositions().get(0).getName());
        assertEquals(1, saveCalls.get());
    }

    @Test
    void updatingReadyToSendOfferReturnsItToDraft() {
        Offer existingOffer = new Offer("business-123");
        existingOffer.markReadyToSend();
        UpdateOfferUseCase useCase = new UpdateOfferUseCase(repositoryFor(existingOffer, new AtomicInteger()));

        Offer result = useCase.execute(updateCommand("business-123"));

        assertEquals(OfferStatus.DRAFT, result.getStatus());
        assertEquals(3, result.getStatusHistory().size());
        assertEquals(OfferStatus.DRAFT, result.getStatusHistory().get(2).getStatus());
    }

    @Test
    void rejectsUpdateOfSentOfferUntilCustomerRequestsChanges() {
        Offer existingOffer = new Offer("business-123");
        existingOffer.markReadyToSend();
        existingOffer.markSent();
        AtomicInteger saveCalls = new AtomicInteger();
        UpdateOfferUseCase useCase = new UpdateOfferUseCase(repositoryFor(existingOffer, saveCalls));

        assertThrows(IllegalStateException.class, () -> useCase.execute(updateCommand("business-123")));

        assertEquals(OfferStatus.SENT, existingOffer.getStatus());
        assertEquals(0, saveCalls.get());
    }

    @Test
    void throwsWhenOfferDoesNotExist() {
        UpdateOfferUseCase useCase = new UpdateOfferUseCase(repositoryFor(null, new AtomicInteger()));

        assertThrows(NoSuchElementException.class,
                () -> useCase.execute(updateCommand("missing-offer")));
    }

    private UpdateOfferCommand updateCommand(String businessKey) {
        CreateOfferPositionCommand position = new CreateOfferPositionCommand(
                OfferPositionType.MATERIAL,
                "Wood",
                "Oak board",
                "Manufacturer",
                new BigDecimal("2"),
                "meter",
                "catalog-123",
                new BigDecimal("12.50"),
                1);
        return new UpdateOfferCommand(
                businessKey,
                "customer-456",
                "craftsman-789",
                "accept-token",
                new BigDecimal("2.5"),
                List.of("Confirm measurements"),
                List.of(position));
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
