package de.werkmundo.offer.domain;

public class OfferAlreadyExistsException extends IllegalStateException {
    public OfferAlreadyExistsException() {
        super("An offer with this business key already exists");
    }
}
