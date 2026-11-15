package de.werkmundo.document.application.port;

import de.werkmundo.document.api.dto.DocumentSnapshotRequest;
import de.werkmundo.document.domain.DocumentType;

public interface DocumentPdfGenerator {
    byte[] generate(DocumentType type, DocumentSnapshotRequest snapshot);
}
