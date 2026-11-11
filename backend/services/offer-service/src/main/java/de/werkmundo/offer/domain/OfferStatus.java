package de.werkmundo.offer.domain;

public enum OfferStatus {
    PROCESSING,
    DRAFT,
    READY_TO_SEND,
    SENT,
    ACCEPTED_BY_CUSTOMER,
    REJECTED_BY_CUSTOMER,
    CHANGE_REQUESTED,
    CANCELLED
}