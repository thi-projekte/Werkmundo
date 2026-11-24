package de.werkmundo.invoice.document.application.port;

import de.werkmundo.invoice.document.domain.DocumentSnapshot;
import de.werkmundo.invoice.document.domain.DocumentType;

public interface DocumentPdfGenerator {
    byte[] generate(DocumentType type, DocumentSnapshot snapshot);
}
