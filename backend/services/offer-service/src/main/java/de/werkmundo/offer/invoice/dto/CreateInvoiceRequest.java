package de.werkmundo.offer.invoice.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request-Body für POST /rechnungen.
 */
public class CreateInvoiceRequest {

    @NotBlank(message = "businessKey darf nicht leer sein")
    public String businessKey;
}
