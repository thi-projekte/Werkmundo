package de.werkmundo.offer.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Optional;
import de.werkmundo.offer.domain.Offer;
import de.werkmundo.offer.domain.OfferPositionType;
import de.werkmundo.offer.domain.OfferRepository;
import de.werkmundo.offer.domain.OfferStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class CreateOfferUseCaseTest {

    @Test
    void createsDraftOfferAndSavesIt() {
        AtomicReference<Offer> savedOffer = new AtomicReference<>();
        OfferRepository repository = new OfferRepository() {
            @Override
            public Offer save(Offer offer) {
                savedOffer.set(offer);
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
        };

        CreateOfferUseCase useCase = new CreateOfferUseCase(repository);
        CreateOfferPositionCommand position = new CreateOfferPositionCommand(
                OfferPositionType.MATERIAL,
                "Wood",
                "Oak board",
                "Example Manufacturer",
                new BigDecimal("2"),
                "meter",
                "catalog-123",
                new BigDecimal("12.50"),
                1);
        CreateOfferCommand command = new CreateOfferCommand(
                "business-123",
                "customer-456",
                "craftsman-789",
                "accept-token",
                new BigDecimal("2.5"),
                List.of("Confirm measurements"),
                List.of(position));

        Offer result = useCase.execute(command);

        assertSame(savedOffer.get(), result);
        assertEquals("business-123", result.getBusinessKey());
        assertEquals("customer-456", result.getCustomerId());
        assertEquals("craftsman-789", result.getCraftsmanId());
        assertEquals("accept-token", result.getAcceptanceToken());
        assertEquals(new BigDecimal("25.00"), result.getTotalPrice());
        assertEquals(new BigDecimal("2.5"), result.getEstimatedWorkDurationHours());
        assertEquals(List.of("Confirm measurements"), result.getCorrectionSuggestions());
        assertEquals(OfferStatus.DRAFT, result.getStatus());
        assertEquals(1, result.getPositions().size());
        assertSame(result, result.getPositions().get(0).getOffer());
        assertEquals("Wood", result.getPositions().get(0).getName());
        assertEquals(new BigDecimal("25.00"), result.getPositions().get(0).getLineTotal());
    }
}
