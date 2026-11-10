
package de.werkmundo.offer.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class OfferEntityRepository
        implements PanacheRepository<OfferEntity> {

    public Optional<OfferEntity> findByBusinessKey(String businessKey) {
        return find("businessKey", businessKey).firstResultOptional();
    }

    public List<OfferEntity> findByCraftsmanId(String craftsmanId) {
        return list("craftsmanId", craftsmanId);
    }
}
