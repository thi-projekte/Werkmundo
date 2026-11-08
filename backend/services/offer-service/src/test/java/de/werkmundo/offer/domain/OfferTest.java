package de.werkmundo.offer.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class OfferTest {

    @Test
    void newOfferStartsAsDraftAndRecordsInitialStatus() {
        Offer offer = new Offer("business-123");

        assertEquals("business-123", offer.getBusinessKey());
        assertEquals(OfferStatus.DRAFT, offer.getStatus());
        assertEquals(1, offer.getStatusHistory().size());
        assertEquals(OfferStatus.DRAFT, offer.getStatusHistory().get(0).getStatus());
        assertNotNull(offer.getCreatedAt());
        assertNotNull(offer.getUpdatedAt());
    }

    @Test
    void changingStatusAddsStatusHistoryEntry() {
        Offer offer = new Offer("business-123");

        offer.markReadyToSend();

        assertEquals(OfferStatus.READY_TO_SEND, offer.getStatus());
        assertEquals(2, offer.getStatusHistory().size());
        assertEquals(OfferStatus.READY_TO_SEND, offer.getStatusHistory().get(1).getStatus());
        assertNotNull(offer.getStatusHistory().get(1).getChangedAt());
    }

    @Test
    void customerCanRequestChangesAfterOfferWasSent() {
        Offer offer = new Offer("business-123");
        offer.markReadyToSend();
        offer.markSent();

        offer.requestChanges("Please update the delivery date");

        assertEquals(OfferStatus.CHANGE_REQUESTED, offer.getStatus());
        assertEquals(List.of("Please update the delivery date"), offer.getCustomerChangeRequests());
        assertEquals(4, offer.getStatusHistory().size());
    }

    @Test
    void offerCannotBeAcceptedBeforeItIsSent() {
        Offer offer = new Offer("business-123");

        assertThrows(IllegalStateException.class, offer::acceptByCustomer);
        assertEquals(OfferStatus.DRAFT, offer.getStatus());
    }

    @Test
    void customerCanRejectSentOffer() {
        Offer offer = new Offer("business-123");
        offer.markReadyToSend();
        offer.markSent();

        offer.rejectByCustomer();

        assertEquals(OfferStatus.REJECTED_BY_CUSTOMER, offer.getStatus());
    }

    @Test
    void offerCanBeCancelledBeforeCustomerDecision() {
        Offer offer = new Offer("business-123");

        offer.cancel();

        assertEquals(OfferStatus.CANCELLED, offer.getStatus());
        assertThrows(IllegalStateException.class, offer::cancel);
    }

    @Test
    void offerDetailsCanOnlyBeChangedBeforeSendingOrAfterChangeRequest() {
        Offer offer = new Offer("business-123");
        offer.markReadyToSend();

        assertThrows(IllegalStateException.class, () -> offer.setEstimatedWorkDurationHours(BigDecimal.TEN));
        offer.markSent();
        assertThrows(IllegalStateException.class, () -> offer.setEstimatedWorkDurationHours(BigDecimal.ONE));
    }

    @Test
    void offerStoresPositionsAndCorrectionSuggestions() {
        Offer offer = new Offer("business-123");
        OfferPosition position = new OfferPosition(
                OfferPositionType.LABOR,
                "Installation",
                "Install the equipment",
                "Werkmundo",
                BigDecimal.ONE,
                "hour",
                null,
                new BigDecimal("75.00"),
                1);

        offer.addPosition(position);
        offer.addCorrectionSuggestion("Confirm installation date");

        assertEquals(List.of(position), offer.getPositions());
        assertEquals(List.of("Confirm installation date"), offer.getCorrectionSuggestions());
    }

    @Test
    void totalPriceIsCalculatedFromAllPositions() {
        Offer offer = new Offer("business-123");
        offer.addPosition(new OfferPosition(
                OfferPositionType.MATERIAL, "Wood", null, null, new BigDecimal("2"), "meter", null,
                new BigDecimal("12.50"), 1));
        offer.addPosition(new OfferPosition(
                OfferPositionType.LABOR, "Installation", null, null, BigDecimal.ONE, "hour", null,
                new BigDecimal("30.00"), 2));

        assertEquals(new BigDecimal("55.00"), offer.getTotalPrice());
    }

    @Test
    void returnedListsCannotBeModified() {
        Offer offer = new Offer("business-123");

        assertThrows(UnsupportedOperationException.class,
                () -> offer.getPositions().add(new OfferPosition(
                        OfferPositionType.OTHER, "Item", null, null, BigDecimal.ONE, "item", null,
                        BigDecimal.ZERO, 1)));
        assertThrows(UnsupportedOperationException.class,
                () -> offer.getCorrectionSuggestions().add("Suggestion"));
        assertThrows(UnsupportedOperationException.class,
                () -> offer.getStatusHistory().clear());
    }
}
