package de.werkmundo.offer.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OfferPolicyTest {

    @Test
    void allowsExpectedStatusTransitions() {
        assertTrue(OfferPolicy.canTransition(OfferStatus.DRAFT, OfferStatus.READY_TO_SEND));
        assertTrue(OfferPolicy.canTransition(OfferStatus.READY_TO_SEND, OfferStatus.DRAFT));
        assertTrue(OfferPolicy.canTransition(OfferStatus.READY_TO_SEND, OfferStatus.SENT));
        assertTrue(OfferPolicy.canTransition(OfferStatus.SENT, OfferStatus.ACCEPTED_BY_CUSTOMER));
        assertTrue(OfferPolicy.canTransition(OfferStatus.SENT, OfferStatus.REJECTED_BY_CUSTOMER));
        assertTrue(OfferPolicy.canTransition(OfferStatus.SENT, OfferStatus.CHANGE_REQUESTED));
        assertTrue(OfferPolicy.canTransition(OfferStatus.CHANGE_REQUESTED, OfferStatus.DRAFT));
    }

    @Test
    void rejectsInvalidAndFinalStatusTransitions() {
        assertFalse(OfferPolicy.canTransition(OfferStatus.DRAFT, OfferStatus.SENT));
        assertFalse(OfferPolicy.canTransition(OfferStatus.ACCEPTED_BY_CUSTOMER, OfferStatus.CANCELLED));
        assertFalse(OfferPolicy.canTransition(OfferStatus.REJECTED_BY_CUSTOMER, OfferStatus.DRAFT));
        assertFalse(OfferPolicy.canTransition(OfferStatus.CANCELLED, OfferStatus.DRAFT));
    }

    @Test
    void onlyDraftAndOffersWithRequestedChangesCanBeEdited() {
        assertTrue(OfferPolicy.canEdit(OfferStatus.DRAFT));
        assertTrue(OfferPolicy.canEdit(OfferStatus.CHANGE_REQUESTED));
        assertFalse(OfferPolicy.canEdit(OfferStatus.READY_TO_SEND));
        assertFalse(OfferPolicy.canEdit(OfferStatus.SENT));
        assertFalse(OfferPolicy.canEdit(OfferStatus.ACCEPTED_BY_CUSTOMER));
        assertFalse(OfferPolicy.canEdit(OfferStatus.REJECTED_BY_CUSTOMER));
        assertFalse(OfferPolicy.canEdit(OfferStatus.CANCELLED));
    }
}
