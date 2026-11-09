package de.winfprojekt.craftvoice.offerservice.offer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request-DTO zum Speichern eines Angebotsentwurfs ohne Kunden.
 */
public class SaveOfferDraftRequest {
    @NotBlank
    @Size(max = 255)
    public String title;

    @NotBlank
    public String speechSnippet;
}
