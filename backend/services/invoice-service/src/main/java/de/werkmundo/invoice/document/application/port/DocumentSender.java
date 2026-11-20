package de.werkmundo.invoice.document.application.port;

import de.werkmundo.invoice.document.domain.Document;

public interface DocumentSender {
    void sendToCustomer(Document document);
}
