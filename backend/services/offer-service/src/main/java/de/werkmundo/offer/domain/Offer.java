package de.werkmundo.offer.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Offer {
    private final String businessKey;
    private String customerId;
    private String craftsmanId;
    private String customerAccessTokenHash;
    private Instant customerAccessTokenExpiresAt;
    private BigDecimal estimatedWorkDurationHours;
    private final List<String> correctionSuggestions = new ArrayList<>();
    private final List<String> customerChangeRequests = new ArrayList<>();
    private OfferStatus status;
    private final List<OfferPosition> positions = new ArrayList<>();
    private final List<OfferStatusHistory> statusHistory = new ArrayList<>();
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Offer(String businessKey) {
        this(businessKey, LocalDateTime.now());
    }

    private Offer(String businessKey, LocalDateTime createdAt) {
        this.businessKey = Objects.requireNonNull(businessKey, "businessKey must not be null");
        this.status = OfferStatus.DRAFT;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = createdAt;
        this.statusHistory.add(new OfferStatusHistory(status, createdAt));
    }

    /** Rebuilds an existing offer from persistence without causing status transitions. */
    public static Offer restore(
            String businessKey,
            String customerId,
            String craftsmanId,
            String customerAccessTokenHash,
            Instant customerAccessTokenExpiresAt,
            BigDecimal estimatedWorkDurationHours,
            List<String> correctionSuggestions,
            List<String> customerChangeRequests,
            OfferStatus status,
            List<OfferPosition> positions,
            List<OfferStatusHistory> statusHistory,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        Offer result = new Offer(businessKey, createdAt);
        result.customerId = customerId;
        result.craftsmanId = craftsmanId;
        result.customerAccessTokenHash = customerAccessTokenHash;
        result.customerAccessTokenExpiresAt = customerAccessTokenExpiresAt;
        result.estimatedWorkDurationHours = estimatedWorkDurationHours;
        result.status = Objects.requireNonNull(status, "status must not be null");
        result.correctionSuggestions.addAll(List.copyOf(
                Objects.requireNonNull(correctionSuggestions, "correctionSuggestions must not be null")));
        result.customerChangeRequests.addAll(List.copyOf(
                Objects.requireNonNull(customerChangeRequests, "customerChangeRequests must not be null")));
        for (OfferPosition position : Objects.requireNonNull(positions, "positions must not be null")) {
            position.setOffer(result);
            result.positions.add(position);
        }
        result.statusHistory.clear();
        result.statusHistory.addAll(List.copyOf(
                Objects.requireNonNull(statusHistory, "statusHistory must not be null")));
        result.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        return result;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        requireEditable();
        this.customerId = customerId;
        updateTimestamp();
    }

    public String getCraftsmanId() {
        return craftsmanId;
    }

    public void setCraftsmanId(String craftsmanId) {
        requireEditable();
        this.craftsmanId = craftsmanId;
        updateTimestamp();
    }

    public String customerAccessTokenHash() {
        return customerAccessTokenHash;
    }

    public Instant customerAccessTokenExpiresAt() {
        return customerAccessTokenExpiresAt;
    }

    public void setCustomerAccessToken(String tokenHash, Instant expiresAt) {
        if (status != OfferStatus.READY_TO_SEND) {
            throw new IllegalStateException("Customer access token can only be issued for approved offers");
        }
        customerAccessTokenHash = Objects.requireNonNull(tokenHash, "tokenHash must not be null");
        customerAccessTokenExpiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        updateTimestamp();
    }

    public boolean hasActiveCustomerAccessTokenAt(Instant now) {
        return customerAccessTokenHash != null
                && customerAccessTokenExpiresAt != null
                && customerAccessTokenExpiresAt.isAfter(Objects.requireNonNull(now, "now must not be null"))
                && (status == OfferStatus.SENT || status == OfferStatus.CHANGE_REQUESTED);
    }

    public BigDecimal getTotalPrice() {
        return positions.stream()
                .map(OfferPosition::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getEstimatedWorkDurationHours() {
        return estimatedWorkDurationHours;
    }

    public void setEstimatedWorkDurationHours(BigDecimal estimatedWorkDurationHours) {
        requireEditable();
        this.estimatedWorkDurationHours = estimatedWorkDurationHours;
        updateTimestamp();
    }

    public List<String> getCorrectionSuggestions() {
        return Collections.unmodifiableList(correctionSuggestions);
    }

    public void addCorrectionSuggestion(String correctionSuggestion) {
        requireEditable();
        correctionSuggestions.add(Objects.requireNonNull(correctionSuggestion, "correctionSuggestion must not be null"));
        updateTimestamp();
    }

    public List<String> getCustomerChangeRequests() {
        return Collections.unmodifiableList(customerChangeRequests);
    }

    public void markReadyToSend() {
        changeStatusTo(OfferStatus.READY_TO_SEND);
    }

    public void markSent() {
        changeStatusTo(OfferStatus.SENT);
    }

    public void acceptByCustomer() {
        changeStatusTo(OfferStatus.ACCEPTED_BY_CUSTOMER);
    }

    public void rejectByCustomer() {
        changeStatusTo(OfferStatus.REJECTED_BY_CUSTOMER);
    }

    public void requestChanges(String changeRequest) {
        Objects.requireNonNull(changeRequest, "changeRequest must not be null");
        requireTransition(OfferStatus.CHANGE_REQUESTED);
        customerChangeRequests.add(changeRequest);
        changeStatusTo(OfferStatus.CHANGE_REQUESTED);
    }

    public void cancel() {
        changeStatusTo(OfferStatus.CANCELLED);
    }

    public OfferStatus getStatus() {
        return status;
    }

    public List<OfferPosition> getPositions() {
        return Collections.unmodifiableList(positions);
    }

    public void addPosition(OfferPosition position) {
        requireEditable();
        OfferPosition newPosition = Objects.requireNonNull(position, "position must not be null");
        newPosition.setOffer(this);
        positions.add(newPosition);
        updateTimestamp();
    }

    public void replacePositions(List<OfferPosition> newPositions) {
        requireEditable();
        List<OfferPosition> positionsToSet = List.copyOf(
                Objects.requireNonNull(newPositions, "positions must not be null"));
        positions.forEach(position -> position.setOffer(null));
        positions.clear();
        positionsToSet.forEach(this::addPosition);
        updateTimestamp();
    }

    public void replaceCorrectionSuggestions(List<String> newCorrectionSuggestions) {
        requireEditable();
        List<String> suggestionsToSet = List.copyOf(
                Objects.requireNonNull(newCorrectionSuggestions, "correctionSuggestions must not be null"));
        correctionSuggestions.clear();
        correctionSuggestions.addAll(suggestionsToSet);
        updateTimestamp();
    }

    public void updateDetailsFrom(Offer updatedOffer) {
        Objects.requireNonNull(updatedOffer, "updatedOffer must not be null");
        if (!businessKey.equals(updatedOffer.businessKey)) {
            throw new IllegalArgumentException("businessKey cannot be changed");
        }
        if (updatedOffer == this) {
            return;
        }
        if (!OfferPolicy.canEdit(status) && status != OfferStatus.READY_TO_SEND) {
            throw new IllegalStateException("Offer cannot be changed in its current status: " + status);
        }
        if (status != OfferStatus.DRAFT) {
            changeStatusTo(OfferStatus.DRAFT);
        }

        customerId = updatedOffer.customerId;
        craftsmanId = updatedOffer.craftsmanId;
        estimatedWorkDurationHours = updatedOffer.estimatedWorkDurationHours;
        correctionSuggestions.clear();
        correctionSuggestions.addAll(updatedOffer.correctionSuggestions);
        replacePositions(updatedOffer.positions);
        updateTimestamp();
    }

    public List<OfferStatusHistory> getStatusHistory() {
        return Collections.unmodifiableList(statusHistory);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    private void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }

    void requireEditable() {
        if (!OfferPolicy.canEdit(status)) {
            throw new IllegalStateException("Offer cannot be changed in its current status: " + status);
        }
    }

    private void requireTransition(OfferStatus newStatus) {
        if (!OfferPolicy.canTransition(status, newStatus)) {
            throw new IllegalStateException("Offer cannot transition from " + status + " to " + newStatus);
        }
    }

    private void changeStatusTo(OfferStatus newStatus) {
        requireTransition(newStatus);
        status = newStatus;
        if (newStatus == OfferStatus.DRAFT
                || newStatus == OfferStatus.ACCEPTED_BY_CUSTOMER
                || newStatus == OfferStatus.REJECTED_BY_CUSTOMER
                || newStatus == OfferStatus.CANCELLED) {
            revokeCustomerAccessToken();
        }
        LocalDateTime changedAt = LocalDateTime.now();
        statusHistory.add(new OfferStatusHistory(newStatus, changedAt));
        updatedAt = changedAt;
    }

    private void revokeCustomerAccessToken() {
        customerAccessTokenHash = null;
        customerAccessTokenExpiresAt = null;
    }
}