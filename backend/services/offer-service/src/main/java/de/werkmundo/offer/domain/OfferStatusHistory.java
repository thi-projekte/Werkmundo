package de.werkmundo.offer.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public class OfferStatusHistory {
    private final OfferStatus status;
    private final LocalDateTime changedAt;

    public OfferStatusHistory(OfferStatus status, LocalDateTime changedAt) {
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.changedAt = Objects.requireNonNull(changedAt, "changedAt must not be null");
    }

    public OfferStatus getStatus() {
        return status;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }
}
