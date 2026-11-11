package de.werkmundo.offer.infrastructure;

import de.werkmundo.offer.domain.Offer;
import de.werkmundo.offer.domain.OfferAlreadyExistsException;
import de.werkmundo.offer.domain.OfferPosition;
import de.werkmundo.offer.domain.OfferPolicy;
import de.werkmundo.offer.domain.OfferPositionType;
import de.werkmundo.offer.domain.OfferRepository;
import de.werkmundo.offer.domain.OfferStatus;
import de.werkmundo.offer.domain.OfferStatusHistory;
import de.winfprojekt.craftvoice.offerservice.offer.OfferStatusPeEvents;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** One repository and one set of tables for both HTTP contracts. */
@ApplicationScoped
@IfBuildProperty(name = "offer.persistence", stringValue = "postgres")
public class OfferJpaRepository implements OfferRepository {
    @Inject EntityManager entityManager;
    @Inject Event<OfferStatusPeEvents.AngebotAngenommen> accepted;
    @Inject Event<OfferStatusPeEvents.AngebotAbgelehnt> rejected;

    private Optional<de.winfprojekt.craftvoice.offerservice.offer.Offer> entity(String key) {
        return entityManager.createQuery("from Offer where businessKey = :key",
                de.winfprojekt.craftvoice.offerservice.offer.Offer.class)
                .setParameter("key", key).getResultStream().findFirst();
    }

    @Override
    @Transactional
    public Offer create(Offer offer) {
        Objects.requireNonNull(offer);
        if (entity(offer.getBusinessKey()).isPresent()) throw new OfferAlreadyExistsException();
        return write(new de.winfprojekt.craftvoice.offerservice.offer.Offer(), offer);
    }

    @Override
    @Transactional
    public Offer save(Offer offer) {
        Objects.requireNonNull(offer);
        return write(entity(offer.getBusinessKey())
                .orElseGet(de.winfprojekt.craftvoice.offerservice.offer.Offer::new), offer);
    }

    private Offer write(de.winfprojekt.craftvoice.offerservice.offer.Offer row, Offer offer) {
        boolean isNew = row.id == null;
        String oldStatus = isNew ? null : row.status;
        if (!isNew && !Objects.equals(row.handwerkerId, offer.getCraftsmanId())) {
            throw new SecurityException("Offer ownership cannot be changed");
        }
        if (offer.getCraftsmanId() == null || offer.getCraftsmanId().isBlank()) {
            throw new IllegalArgumentException("Offer owner is required");
        }
        boolean processing = oldStatus != null && OfferStatusMapper.toDomain(oldStatus) == OfferStatus.PROCESSING;
        if (!isNew) {
            OfferStatus persistedStatus = OfferStatusMapper.toDomain(oldStatus);
            if (persistedStatus != offer.getStatus() && !OfferPolicy.canTransition(persistedStatus, offer.getStatus())) {
                throw new IllegalStateException("Offer state changed since it was loaded");
            }
        }
        row.businessKey = offer.getBusinessKey();
        row.customerId = offer.getCustomerId();
        row.handwerkerId = offer.getCraftsmanId();
        row.customerAccessTokenHash = offer.customerAccessTokenHash();
        row.customerAccessTokenExpiresAt = offer.customerAccessTokenExpiresAt();
        row.geschaetzteArbeitsdauerStunden = offer.getEstimatedWorkDurationHours();
        row.updatedAt = offer.getUpdatedAt();
        row.status = OfferStatusMapper.toProcess(offer.getStatus(), oldStatus);
        if (isNew) {
            row.annahmeToken = UUID.randomUUID().toString();
            row.createdAt = offer.getCreatedAt();
        }

        // Canceling an in-progress offer must not erase partially processed data.
        if (!processing) {
            row.gesamtPreis = offer.getTotalPrice();
            row.korrekturvorschlaege.clear();
            row.korrekturvorschlaege.addAll(offer.getCorrectionSuggestions());
            row.positions.clear();
            for (OfferPosition position : offer.getPositions()) {
                var item = new de.winfprojekt.craftvoice.offerservice.offer.OfferPosition();
                item.offer = row;
                item.type = toProcessType(position.getType());
                item.bezeichnung = position.getName();
                item.beschreibung = position.getDescription();
                item.hersteller = position.getManufacturer();
                item.menge = position.getQuantity();
                item.einheit = position.getUnit();
                item.katalogProduktId = position.getCatalogProductId();
                item.einzelPreis = position.getUnitPrice();
                item.positionsPreis = position.getLineTotal();
                item.reihenfolge = position.getPositionOrder();
                row.positions.add(item);
            }
        }
        row.customerChangeRequests.clear();
        row.customerChangeRequests.addAll(offer.getCustomerChangeRequests());

        // Existing process history, including notes and precise process states, is preserved.
        for (int index = row.statusHistory.size(); index < offer.getStatusHistory().size(); index++) {
            OfferStatusHistory event = offer.getStatusHistory().get(index);
            var history = new de.winfprojekt.craftvoice.offerservice.offer.OfferStatusHistory();
            history.offer = row;
            history.status = OfferStatusMapper.toProcess(event.getStatus(), null);
            history.zeitpunkt = event.getChangedAt();
            row.statusHistory.add(history);
        }
        if (isNew) entityManager.persist(row);
        entityManager.flush();
        if (!Objects.equals(oldStatus, row.status)) {
            if (offer.getStatus() == OfferStatus.ACCEPTED_BY_CUSTOMER) {
                accepted.fire(new OfferStatusPeEvents.AngebotAngenommen(row.businessKey));
            } else if (offer.getStatus() == OfferStatus.REJECTED_BY_CUSTOMER) {
                rejected.fire(new OfferStatusPeEvents.AngebotAbgelehnt(row.businessKey));
            }
        }
        return toDomain(row);
    }

    @Override
    @Transactional
    public Optional<Offer> findByBusinessKey(String key) {
        return entity(Objects.requireNonNull(key)).map(this::toDomain);
    }

    @Override
    @Transactional
    public List<Offer> findAllByCraftsmanId(String owner) {
        return entityManager.createQuery("from Offer where handwerkerId = :owner",
                de.winfprojekt.craftvoice.offerservice.offer.Offer.class)
                .setParameter("owner", Objects.requireNonNull(owner)).getResultStream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public boolean deleteByBusinessKey(String key) {
        var found = entity(Objects.requireNonNull(key));
        if (found.isEmpty()) return false;
        entityManager.remove(found.get());
        entityManager.flush();
        return true;
    }

    private Offer toDomain(de.winfprojekt.craftvoice.offerservice.offer.Offer row) {
        OfferStatus status = OfferStatusMapper.toDomain(row.status);
        List<OfferPosition> positions = status == OfferStatus.PROCESSING ? List.of() : row.positions.stream()
                .sorted(Comparator.comparingInt(item -> item.reihenfolge == null ? 0 : item.reihenfolge))
                .map(item -> {
                    if (item.menge == null || item.einzelPreis == null) {
                        throw new IllegalStateException("Offer positions have not been priced yet");
                    }
                    OfferPosition result = new OfferPosition(toDomainType(item.type), item.bezeichnung,
                            item.beschreibung, item.hersteller, item.menge, item.einheit,
                            item.katalogProduktId, item.einzelPreis, item.reihenfolge == null ? 0 : item.reihenfolge);
                    result.setId(item.id);
                    return result;
                }).toList();
        List<OfferStatusHistory> history = row.statusHistory.stream()
                .sorted(Comparator.comparing(item -> item.zeitpunkt))
                .map(item -> new OfferStatusHistory(OfferStatusMapper.toDomain(item.status), item.zeitpunkt)).toList();
        return Offer.restore(row.businessKey, row.customerId, row.handwerkerId,
                row.customerAccessTokenHash, row.customerAccessTokenExpiresAt, row.geschaetzteArbeitsdauerStunden,
                List.copyOf(row.korrekturvorschlaege), List.copyOf(row.customerChangeRequests), status,
                positions, history, row.createdAt, row.updatedAt);
    }

    private static de.winfprojekt.craftvoice.offerservice.common.OfferPositionType toProcessType(OfferPositionType type) {
        if (type == null) return null;
        return switch (type) {
            case MATERIAL -> de.winfprojekt.craftvoice.offerservice.common.OfferPositionType.MATERIAL;
            case LABOR -> de.winfprojekt.craftvoice.offerservice.common.OfferPositionType.ARBEITSZEIT;
            case TRAVEL -> de.winfprojekt.craftvoice.offerservice.common.OfferPositionType.ANFAHRT;
            case OTHER -> de.winfprojekt.craftvoice.offerservice.common.OfferPositionType.OTHER;
        };
    }

    private static OfferPositionType toDomainType(de.winfprojekt.craftvoice.offerservice.common.OfferPositionType type) {
        if (type == null) return null;
        return switch (type) {
            case MATERIAL -> OfferPositionType.MATERIAL;
            case ARBEITSZEIT -> OfferPositionType.LABOR;
            case ANFAHRT -> OfferPositionType.TRAVEL;
            case OTHER -> OfferPositionType.OTHER;
        };
    }
}