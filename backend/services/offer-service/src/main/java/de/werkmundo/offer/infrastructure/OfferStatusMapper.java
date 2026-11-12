package de.werkmundo.offer.infrastructure;

import de.werkmundo.offer.domain.OfferStatus;
import java.util.Objects;

/** Converts the existing process states without replacing their finer distinctions. */
public final class OfferStatusMapper {
    private OfferStatusMapper() {
    }

    public static OfferStatus toDomain(String processStatus) {
        Objects.requireNonNull(processStatus, "processStatus is required");
        return switch (processStatus) {
            case "ERFASST", "IN_BEARBEITUNG" -> OfferStatus.PROCESSING;
            case "ENTWURF", "KI_FERTIG" -> OfferStatus.DRAFT;
            case "KI_BEARBEITUNG_ABGESCHLOSSEN", "VERSANDBEREIT" -> OfferStatus.READY_TO_SEND;
            case "VERSENDET" -> OfferStatus.SENT;
            case "ANGENOMMEN" -> OfferStatus.ACCEPTED_BY_CUSTOMER;
            case "ABGELEHNT" -> OfferStatus.REJECTED_BY_CUSTOMER;
            case "CHANGE_REQUESTED" -> OfferStatus.CHANGE_REQUESTED;
            case "ABGEBROCHEN", "STORNIERT" -> OfferStatus.CANCELLED;
            default -> throw new IllegalArgumentException("Unsupported process status: " + processStatus);
        };
    }

    public static String toProcess(OfferStatus status, String currentProcessStatus) {
        Objects.requireNonNull(status, "status is required");
        if (currentProcessStatus != null && toDomain(currentProcessStatus) == status) {
            return currentProcessStatus;
        }
        return switch (status) {
            case PROCESSING -> "IN_BEARBEITUNG";
            case DRAFT -> "ENTWURF";
            case READY_TO_SEND -> "KI_BEARBEITUNG_ABGESCHLOSSEN";
            case SENT -> "VERSENDET";
            case ACCEPTED_BY_CUSTOMER -> "ANGENOMMEN";
            case REJECTED_BY_CUSTOMER -> "ABGELEHNT";
            case CHANGE_REQUESTED -> "CHANGE_REQUESTED";
            case CANCELLED -> "STORNIERT";
        };
    }
}