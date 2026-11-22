package de.werkmundo.offer.offer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AssignOfferCustomerRequest(
        @NotNull
        @Positive
        Long customerId
) {
}