package de.werkmundo.document.application.usecase;

import com.fasterxml.jackson.databind.JsonNode;
import de.werkmundo.document.application.port.InvoicePdfGenerator;
import de.werkmundo.document.domain.Document;
import de.werkmundo.document.domain.DocumentRepository;
import de.werkmundo.document.domain.DocumentType;

public class CreateInvoiceDocumentUseCase {
    private final InvoicePdfGenerator pdfGenerator;
    private final DocumentRepository repository;

    public CreateInvoiceDocumentUseCase(InvoicePdfGenerator pdfGenerator, DocumentRepository repository) {
        this.pdfGenerator = pdfGenerator;
        this.repository = repository;
    }

    public String execute(String referenceId, String ownerId, JsonNode invoiceDraft, String recipientEmail) {
        byte[] pdf = pdfGenerator.generate(invoiceDraft);
        if (pdf == null || pdf.length == 0) {
            throw new IllegalStateException("Invoice PDF generator returned no content");
        }

        Document document = new Document(DocumentType.INVOICE, referenceId, ownerId, null, recipientEmail,
                DocumentType.INVOICE.fileName(referenceId), pdf);
        return repository.save(document);
    }
}
