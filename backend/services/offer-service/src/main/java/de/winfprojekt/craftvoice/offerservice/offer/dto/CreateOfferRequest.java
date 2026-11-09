package de.winfprojekt.craftvoice.offerservice.offer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request-DTO zum Erstellen eines neuen Angebots.
 */
public class CreateOfferRequest {
    @NotNull
    public String customerId;

    @NotNull
    public String speechSnippet;

    @Size(max = 255)
    public String title;
    // hier könnte eine oder ggf. mehrere Vorlagen als zusätzlicher Input mitgegeben werden
}