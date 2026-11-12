package de.werkmundo.offer.infrastructure;

import de.werkmundo.offer.domain.OfferPolicy;
import de.werkmundo.offer.domain.OfferStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OfferStatusMapperTest {
    @Test
    void preservesEveryExistingProcessStateWhenTheDomainStatusIsUnchanged() {
        for (String state : new String[]{"ERFASST", "IN_BEARBEITUNG", "ENTWURF", "KI_FERTIG",
                "KI_BEARBEITUNG_ABGESCHLOSSEN", "VERSANDBEREIT", "VERSENDET", "ANGENOMMEN",
                "ABGELEHNT", "CHANGE_REQUESTED", "ABGEBROCHEN", "STORNIERT"}) {
            assertEquals(state, OfferStatusMapper.toProcess(OfferStatusMapper.toDomain(state), state));
        }
    }

    @Test
    void mapsImportantBusinessTransitionsExplicitly() {
        assertEquals("VERSENDET", OfferStatusMapper.toProcess(OfferStatus.SENT, "VERSANDBEREIT"));
        assertEquals("ANGENOMMEN", OfferStatusMapper.toProcess(OfferStatus.ACCEPTED_BY_CUSTOMER, "VERSENDET"));
        assertEquals("ABGELEHNT", OfferStatusMapper.toProcess(OfferStatus.REJECTED_BY_CUSTOMER, "VERSENDET"));
        assertEquals("ENTWURF", OfferStatusMapper.toProcess(OfferStatus.DRAFT, "CHANGE_REQUESTED"));
    }

    @Test
    void neverTreatsAnInProgressOfferAsAnEditableDraft() {
        assertEquals(OfferStatus.PROCESSING, OfferStatusMapper.toDomain("IN_BEARBEITUNG"));
        assertFalse(OfferPolicy.canEdit(OfferStatus.PROCESSING));
        assertFalse(OfferPolicy.canTransition(OfferStatus.PROCESSING, OfferStatus.READY_TO_SEND));
        assertFalse(OfferPolicy.canTransition(OfferStatus.PROCESSING, OfferStatus.ACCEPTED_BY_CUSTOMER));
        assertTrue(OfferPolicy.canTransition(OfferStatus.PROCESSING, OfferStatus.CANCELLED));
    }

    @Test
    void rejectsUnknownStatesInsteadOfSilentlyConvertingThemToDrafts() {
        assertThrows(IllegalArgumentException.class, () -> OfferStatusMapper.toDomain("UNKNOWN"));
        assertThrows(IllegalArgumentException.class,
                () -> OfferStatusMapper.toProcess(OfferStatus.DRAFT, "UNKNOWN"));
        assertThrows(NullPointerException.class, () -> OfferStatusMapper.toDomain(null));
    }
}