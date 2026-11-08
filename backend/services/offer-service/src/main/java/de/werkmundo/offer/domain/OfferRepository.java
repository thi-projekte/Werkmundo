package de.werkmundo.offer.domain;

import java.util.List;
import java.util.Optional;

public interface OfferRepository {
    Offer save(Offer offer);

    Optional<Offer> findByBusinessKey(String businessKey);

    List<Offer> findAllByCraftsmanId(String craftsmanId);
}