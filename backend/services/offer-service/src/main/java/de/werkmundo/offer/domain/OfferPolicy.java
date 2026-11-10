package de.werkmundo.offer.domain;

public final class OfferPolicy {
    private OfferPolicy() {
    }

    public static boolean canEdit(OfferStatus status) {
        return status == OfferStatus.DRAFT
                || status == OfferStatus.CHANGE_REQUESTED;
    }

    public static boolean canTransition(OfferStatus currentStatus, OfferStatus newStatus) {
        if (currentStatus == null || newStatus == null) {
            return false;
        }

        return switch (currentStatus) {
            case DRAFT -> newStatus == OfferStatus.READY_TO_SEND || newStatus == OfferStatus.CANCELLED;
            case READY_TO_SEND -> newStatus == OfferStatus.DRAFT
                    || newStatus == OfferStatus.SENT
                    || newStatus == OfferStatus.CANCELLED;
            case SENT -> newStatus == OfferStatus.ACCEPTED_BY_CUSTOMER
                    || newStatus == OfferStatus.REJECTED_BY_CUSTOMER
                    || newStatus == OfferStatus.CHANGE_REQUESTED
                    || newStatus == OfferStatus.CANCELLED;
            case CHANGE_REQUESTED -> newStatus == OfferStatus.DRAFT || newStatus == OfferStatus.CANCELLED;
            case ACCEPTED_BY_CUSTOMER, REJECTED_BY_CUSTOMER, CANCELLED -> false;
        };
    }
}