package de.werkmundo.offer.infrastructure;

import de.werkmundo.offer.domain.Offer;
import de.werkmundo.offer.domain.OfferRepository;
import io.quarkus.arc.properties.UnlessBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@ApplicationScoped
@UnlessBuildProperty(name = "offer.persistence", stringValue = "postgres")
public class DevelopmentInMemoryOfferRepository implements OfferRepository {
    private final ConcurrentMap<String, Offer> offers = new ConcurrentHashMap<>();

    @Override
    public Offer save(Offer offer) {
        Offer savedOffer = Objects.requireNonNull(offer, "offer must not be null");
        offers.put(savedOffer.getBusinessKey(), savedOffer);
        return savedOffer;
    }

    @Override
    public Optional<Offer> findByBusinessKey(String businessKey) {
        return Optional.ofNullable(offers.get(Objects.requireNonNull(businessKey, "businessKey must not be null")));
    }

    @Override
    public List<Offer> findAllByCraftsmanId(String craftsmanId) {
        String ownerId = Objects.requireNonNull(craftsmanId, "craftsmanId must not be null");
        return offers.values().stream()
                .filter(offer -> ownerId.equals(offer.getCraftsmanId()))
                .toList();
    }
}
