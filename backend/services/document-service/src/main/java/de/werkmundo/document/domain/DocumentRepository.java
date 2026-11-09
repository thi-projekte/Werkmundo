package de.werkmundo.document.domain;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository {
    String save(Document document);

    Optional<DocumentMetadata> findMetadataByIdAndOwner(String documentId, String ownerId);

    List<DocumentMetadata> findMetadataByOwner(String ownerId);

    Optional<DocumentMetadata> findLatestMetadataByReferenceAndOwner(
            DocumentType type, String referenceId, String ownerId);

    Optional<Document> findByIdAndOwner(String documentId, String ownerId);
}
