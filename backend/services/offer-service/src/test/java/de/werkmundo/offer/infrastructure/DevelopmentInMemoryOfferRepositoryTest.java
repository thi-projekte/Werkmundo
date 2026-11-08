package de.werkmundo.offer.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.werkmundo.offer.domain.Offer;
import java.util.List;
import org.junit.jupiter.api.Test;

class DevelopmentInMemoryOfferRepositoryTest {
    @Test
    void savesAndFindsOffersByBusinessKey() {
        DevelopmentInMemoryOfferRepository repository = new DevelopmentInMemoryOfferRepository();
        Offer offer = new Offer("offer-1");

        assertSame(offer, repository.save(offer));
        assertSame(offer, repository.findByBusinessKey("offer-1").orElseThrow());
    }

    @Test
    void listsOnlyOffersForRequestedCraftsman() {
        DevelopmentInMemoryOfferRepository repository = new DevelopmentInMemoryOfferRepository();
        Offer firstOffer = new Offer("offer-1");
        firstOffer.setCraftsmanId("craftsman-1");
        Offer secondOffer = new Offer("offer-2");
        secondOffer.setCraftsmanId("craftsman-2");
        repository.save(firstOffer);
        repository.save(secondOffer);

        List<Offer> result = repository.findAllByCraftsmanId("craftsman-1");

        assertEquals(1, result.size());
        assertSame(firstOffer, result.get(0));
        assertTrue(repository.findByBusinessKey("missing").isEmpty());
    }
}
