package de.werkmundo.offer.infrastructure;

import de.werkmundo.offer.domain.Offer;
import de.werkmundo.offer.domain.OfferPosition;
import de.werkmundo.offer.domain.OfferPositionType;
import de.werkmundo.offer.domain.OfferRepository;
import de.werkmundo.offer.domain.OfferStatus;
import de.werkmundo.offer.domain.OfferStatusHistory;
import de.werkmundo.offer.infrastructure.persistence.OfferEntity;
import de.werkmundo.offer.infrastructure.persistence.OfferEntityRepository;
import de.werkmundo.offer.infrastructure.persistence.OfferPositionEntity;
import de.werkmundo.offer.infrastructure.persistence.OfferStatusHistoryEntity;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** PostgreSQL-backed implementation of the domain repository, enabled by offer.persistence=postgres. */
@ApplicationScoped
@IfBuildProperty(name = "offer.persistence", stringValue = "postgres")
public class OfferJpaRepository implements OfferRepository {

    @Inject
    OfferEntityRepository entities;

    @Override
    @Transactional
    public Offer save(Offer offer) {
        Objects.requireNonNull(offer, "offer must not be null");
        OfferEntity entity = entities.findByBusinessKey(offer.getBusinessKey())
                .orElseGet(OfferEntity::new);
        entity.businessKey = offer.getBusinessKey();
        entity.customerId = offer.getCustomerId();
        entity.craftsmanId = offer.getCraftsmanId();
        entity.customerAccessTokenHash = offer.customerAccessTokenHash();
        entity.customerAccessTokenExpiresAt = offer.customerAccessTokenExpiresAt() == null
                ? null
                : java.time.LocalDateTime.ofInstant(offer.customerAccessTokenExpiresAt(), ZoneOffset.UTC);
        entity.estimatedWorkDurationHours = offer.getEstimatedWorkDurationHours();
        entity.status = offer.getStatus().name();
        entity.createdAt = offer.getCreatedAt();
        entity.updatedAt = offer.getUpdatedAt();

        entity.positions.clear();
        for (OfferPosition position : offer.getPositions()) {
            OfferPositionEntity row = new OfferPositionEntity();
            row.offer = entity;
            row.type = position.getType() == null ? null : position.getType().name();
            row.name = position.getName();
            row.description = position.getDescription();
            row.manufacturer = position.getManufacturer();
            row.quantity = position.getQuantity();
            row.unit = position.getUnit();
            row.catalogProductId = position.getCatalogProductId();
            row.unitPrice = position.getUnitPrice();
            row.positionOrder = position.getPositionOrder();
            entity.positions.add(row);
        }

        entity.statusHistory.clear();
        for (OfferStatusHistory history : offer.getStatusHistory()) {
            OfferStatusHistoryEntity row = new OfferStatusHistoryEntity();
            row.offer = entity;
            row.status = history.getStatus().name();
            row.changedAt = history.getChangedAt();
            entity.statusHistory.add(row);
        }

        entity.correctionSuggestions.clear();
        entity.correctionSuggestions.addAll(offer.getCorrectionSuggestions());
        entity.customerChangeRequests.clear();
        entity.customerChangeRequests.addAll(offer.getCustomerChangeRequests());

        if (entity.id == null) {
            entities.persist(entity);
        }
        entities.flush();
        return toDomain(entity);
    }

    @Override
    @Transactional
    public Optional<Offer> findByBusinessKey(String businessKey) {
        Objects.requireNonNull(businessKey, "businessKey must not be null");
        return entities.findByBusinessKey(businessKey).map(this::toDomain);
    }

    @Override
    @Transactional
    public List<Offer> findAllByCraftsmanId(String craftsmanId) {
        Objects.requireNonNull(craftsmanId, "craftsmanId must not be null");
        return entities.findByCraftsmanId(craftsmanId).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public boolean deleteByBusinessKey(String businessKey) {
        Objects.requireNonNull(businessKey, "businessKey must not be null");
        return entities.findByBusinessKey(businessKey)
                .map(entity -> {
                    entities.delete(entity);
                    entities.flush();
                    return true;
                })
                .orElse(false);
    }

    private Offer toDomain(OfferEntity row) {
        List<OfferPosition> positions = row.positions.stream().map(position -> {
            OfferPosition restored = new OfferPosition(
                    position.type == null ? null : OfferPositionType.valueOf(position.type),
                    position.name, position.description, position.manufacturer,
                    position.quantity, position.unit, position.catalogProductId,
                    position.unitPrice, position.positionOrder);
            restored.setId(position.id);
            return restored;
        }).toList();
        List<OfferStatusHistory> history = row.statusHistory.stream()
                .map(item -> new OfferStatusHistory(OfferStatus.valueOf(item.status), item.changedAt))
                .toList();
        return Offer.restore(
                row.businessKey, row.customerId, row.craftsmanId,
                row.customerAccessTokenHash,
                row.customerAccessTokenExpiresAt == null
                        ? null : row.customerAccessTokenExpiresAt.toInstant(ZoneOffset.UTC),
                row.estimatedWorkDurationHours,
                row.correctionSuggestions, row.customerChangeRequests,
                OfferStatus.valueOf(row.status), positions, history,
                row.createdAt, row.updatedAt);
    }
}
