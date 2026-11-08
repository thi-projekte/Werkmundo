package de.werkmundo.offer.application;

import de.werkmundo.offer.application.usecase.offer.ListOffersUseCase;
import de.werkmundo.offer.application.usecase.offer.OfferSortOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;

import de.werkmundo.offer.domain.Offer;
import de.werkmundo.offer.domain.OfferPosition;
import de.werkmundo.offer.domain.OfferPositionType;
import de.werkmundo.offer.domain.OfferRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ListOffersUseCaseTest {

    @Test
    void listsOnlyCraftsmanOffersSortedByPrice() {
        Offer inexpensiveOffer = createOffer("offer-1", "craftsman-1", "10.00");
        Offer expensiveOffer = createOffer("offer-2", "craftsman-1", "30.00");
        Offer anotherCraftsmanOffer = createOffer("offer-3", "craftsman-2", "5.00");
        List<Offer> offers = List.of(expensiveOffer, anotherCraftsmanOffer, inexpensiveOffer);

        ListOffersUseCase useCase = new ListOffersUseCase(repositoryFor(offers));

        List<Offer> result = useCase.execute("craftsman-1", OfferSortOrder.PRICE_LOW_TO_HIGH);

        assertEquals(List.of(inexpensiveOffer, expensiveOffer), result);
    }

    @Test
    void sortsOffersByCreationDateNewestFirstByDefault() {
        Offer olderOffer = createOffer(
                "offer-1", "craftsman-1", "10.00", LocalDateTime.of(2025, 1, 1, 0, 0));
        Offer newerOffer = createOffer(
                "offer-2", "craftsman-1", "30.00", LocalDateTime.of(2025, 1, 2, 0, 0));
        ListOffersUseCase useCase = new ListOffersUseCase(repositoryFor(List.of(olderOffer, newerOffer)));

        assertEquals(List.of(newerOffer, olderOffer), useCase.execute("craftsman-1"));
    }

    private Offer createOffer(String businessKey, String craftsmanId, String price) {
        return createOffer(businessKey, craftsmanId, price, LocalDateTime.now());
    }

    private Offer createOffer(String businessKey, String craftsmanId, String price, LocalDateTime createdAt) {
        Offer offer = new Offer(businessKey) {
            @Override
            public LocalDateTime getCreatedAt() {
                return createdAt;
            }
        };
        offer.setCraftsmanId(craftsmanId);
        offer.addPosition(new OfferPosition(
                OfferPositionType.MATERIAL,
                "Item",
                null,
                null,
                BigDecimal.ONE,
                "item",
                null,
                new BigDecimal(price),
                1));
        return offer;
    }

    private OfferRepository repositoryFor(List<Offer> offers) {
        return new OfferRepository() {
            @Override
            public Offer save(Offer offer) {
                return offer;
            }

            @Override
            public Optional<Offer> findByBusinessKey(String businessKey) {
                return offers.stream()
                        .filter(offer -> offer.getBusinessKey().equals(businessKey))
                        .findFirst();
            }

            @Override
            public List<Offer> findAllByCraftsmanId(String craftsmanId) {
                return offers.stream()
                        .filter(offer -> craftsmanId.equals(offer.getCraftsmanId()))
                        .toList();
            }
        };
    }
}
