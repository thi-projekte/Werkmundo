package de.werkmundo.invoice.document.domain;

public record Document(
        DocumentType type, String referenceId, String ownerId, String customerId, String recipientEmail,
        String fileName, byte[] pdfContent) {
}
