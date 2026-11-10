package de.werkmundo.offer.application;

import de.werkmundo.offer.application.usecase.offer.GetOfferUseCase;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.werkmundo.offer.domain.Offer;
import de.werkmundo.offer.domain.OfferRepository;
import java.util.NoSuchElementException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetOfferUseCaseTest {

    @Test
    void returnsOfferOwnedByCraftsman() {
        Offer offer = new Offer("offer-123");
        offer.setCraftsmanId("craftsman-456");
        GetOfferUseCase useCase = new GetOfferUseCase(repositoryFor(offer));

        Offer result = useCase.execute("offer-123", "craftsman-456");

        assertSame(offer, result);
    }

    @Test
    void doesNotReturnOfferOwnedByAnotherCraftsman() {
        Offer offer = new Offer("offer-123");
        offer.setCraftsmanId("craftsman-456");
        GetOfferUseCase useCase = new GetOfferUseCase(repositoryFor(offer));

        assertThrows(NoSuchElementException.class,
                () -> useCase.execute("offer-123", "craftsman-789"));
    }

    @Test
    void throwsWhenOfferDoesNotExist() {
        GetOfferUseCase useCase = new GetOfferUseCase(new OfferRepository() {
            @Override
            public Offer save(Offer offer) {
                return offer;
            }

            @Override
            public Optional<Offer> findByBusinessKey(String businessKey) {
                return Optional.empty();
            }

            @Override
            public List<Offer> findAllByCraftsmanId(String craftsmanId) {
                return List.of();
            }
        });

        assertThrows(NoSuchElementException.class,
                () -> useCase.execute("missing-offer", "craftsman-456"));
    }

    private OfferRepository repositoryFor(Offer offer) {
        return new OfferRepository() {
            @Override
            public Offer save(Offer offerToSave) {
                return offerToSave;
            }

            @Override
            public Optional<Offer> findByBusinessKey(String businessKey) {
                return businessKey.equals(offer.getBusinessKey()) ? Optional.of(offer) : Optional.empty();
            }

            @Override
            public List<Offer> findAllByCraftsmanId(String craftsmanId) {
                return List.of();
            }
        };
    }
}
