package de.werkmundo.invoice.document.application.usecase;

import de.werkmundo.invoice.document.application.port.DocumentPdfGenerator;
import de.werkmundo.invoice.document.domain.Document;
import de.werkmundo.invoice.document.domain.DocumentRepository;
import de.werkmundo.invoice.document.domain.DocumentSnapshot;
import de.werkmundo.invoice.document.domain.DocumentType;

public class CreateOfferDocumentUseCase {
    private final DocumentPdfGenerator pdfGenerator;
    private final DocumentRepository repository;

    public CreateOfferDocumentUseCase(DocumentPdfGenerator pdfGenerator, DocumentRepository repository) {
        this.pdfGenerator = pdfGenerator;
        this.repository = repository;
    }

    public String execute(
            String businessKey,
            String ownerId,
            String customerId,
            String recipientEmail,
            DocumentSnapshot snapshot) {
        byte[] pdf = pdfGenerator.generate(DocumentType.OFFER, snapshot);
        if (pdf == null || pdf.length == 0) {
            throw new IllegalStateException("Offer PDF generator returned no content");
        }

        Document document = new Document(
                DocumentType.OFFER,
                businessKey,
                ownerId,
                customerId,
                recipientEmail,
                DocumentType.OFFER.fileName(businessKey),
                pdf);
        return repository.save(document);
    }
}
