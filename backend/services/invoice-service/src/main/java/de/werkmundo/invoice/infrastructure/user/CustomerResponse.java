package de.werkmundo.invoice.infrastructure.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CustomerResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        String street,
        String houseNumber,
        String zipCode,
        String city
) {
}