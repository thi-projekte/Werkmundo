package de.werkmundo.document.application.usecase;

import de.werkmundo.document.api.dto.DocumentSnapshotRequest;
import de.werkmundo.document.application.port.DocumentPdfGenerator;
import de.werkmundo.document.domain.Document;
import de.werkmundo.document.domain.DocumentRepository;
import de.werkmundo.document.domain.DocumentType;

public class CreateDocumentFromSnapshotUseCase {
    private final DocumentPdfGenerator pdfGenerator;
    private final DocumentRepository repository;

    public CreateDocumentFromSnapshotUseCase(DocumentPdfGenerator pdfGenerator, DocumentRepository repository) {
        this.pdfGenerator = pdfGenerator;
        this.repository = repository;
    }

    public String execute(
            DocumentType type, String referenceId, String ownerId, DocumentSnapshotRequest snapshot) {
        byte[] pdf = pdfGenerator.generate(type, snapshot);
        if (pdf == null || pdf.length == 0) {
            throw new IllegalStateException("Document PDF generator returned no content");
        }

        Document document = new Document(
                type,
                referenceId,
                ownerId,
                snapshot.customerId(),
                snapshot.customer().email(),
                type.fileName(referenceId),
                pdf);
        return repository.save(document);
    }
}
