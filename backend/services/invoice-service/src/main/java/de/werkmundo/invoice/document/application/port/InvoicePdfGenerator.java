package de.werkmundo.invoice.document.application.port;

import com.fasterxml.jackson.databind.JsonNode;

public interface InvoicePdfGenerator {
    byte[] generate(JsonNode invoiceDraft);
}
