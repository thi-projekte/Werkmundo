package de.werkmundo.document.application.port;

import de.werkmundo.document.domain.Document;

public interface DocumentSender {
    void sendToCustomer(Document document);
}
