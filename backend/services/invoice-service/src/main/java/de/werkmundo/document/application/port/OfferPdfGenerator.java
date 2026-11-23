package de.werkmundo.document.application.port;

import com.fasterxml.jackson.databind.JsonNode;

public interface OfferPdfGenerator {
    byte[] generate(JsonNode offer, boolean demo);
}
