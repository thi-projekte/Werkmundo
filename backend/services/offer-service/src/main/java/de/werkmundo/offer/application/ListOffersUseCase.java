package de.werkmundo.offer.application;

import de.werkmundo.offer.domain.Offer;
import de.werkmundo.offer.domain.OfferRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class ListOffersUseCase {
    private final OfferRepository offerRepository;

    public ListOffersUseCase(OfferRepository offerRepository) {
        this.offerRepository = Objects.requireNonNull(offerRepository, "offerRepository must not be null");
    }

    public List<Offer> execute(String craftsmanId) {
        return execute(craftsmanId, OfferSortOrder.NEWEST_FIRST);
    }

    public List<Offer> execute(String craftsmanId, OfferSortOrder sortOrder) {
        Objects.requireNonNull(craftsmanId, "craftsmanId must not be null");
        Objects.requireNonNull(sortOrder, "sortOrder must not be null");

        Comparator<Offer> comparator = switch (sortOrder) {
            case NEWEST_FIRST -> Comparator.comparing(Offer::getCreatedAt).reversed();
            case OLDEST_FIRST -> Comparator.comparing(Offer::getCreatedAt);
            case PRICE_LOW_TO_HIGH -> Comparator.comparing(Offer::getTotalPrice);
            case PRICE_HIGH_TO_LOW -> Comparator.comparing(Offer::getTotalPrice).reversed();
        };

        return offerRepository.findAllByCraftsmanId(craftsmanId).stream()
                .sorted(comparator)
                .toList();
    }
}
