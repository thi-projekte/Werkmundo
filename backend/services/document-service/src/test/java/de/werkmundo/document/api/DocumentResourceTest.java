package de.werkmundo.document.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.werkmundo.document.api.dto.DocumentShareRequest;
import de.werkmundo.document.api.dto.DocumentSnapshotRequest;
import de.werkmundo.document.application.port.DocumentPdfGenerator;
import de.werkmundo.document.application.port.DocumentSender;
import de.werkmundo.document.domain.Document;
import de.werkmundo.document.domain.DocumentMetadata;
import de.werkmundo.document.domain.DocumentRepository;
import de.werkmundo.document.domain.DocumentType;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DocumentResourceTest {
    private final InMemoryDocumentRepository repository = new InMemoryDocumentRepository();
    private final List<SentMail> sentMail = new ArrayList<>();
    private final DocumentPdfGenerator pdfGenerator = (type, snapshot) -> (type.name() + " pdf").getBytes();
    private final DocumentSender sender = (document, accessLink) -> sentMail.add(new SentMail(document, accessLink));
    private final DocumentResource resource = resource("handwerker-42");

    @Test
    void createsAndSharesOfferWithItsAccessLink() {
        Response response = resource.generateOfferDocument("angebot-123", snapshot());
        String documentId = ((DocumentResource.CreatedDocument) response.getEntity()).documentId();

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        Response shareResponse = resource.shareOffer(
                "angebot-123", new DocumentShareRequest("https://offers.example.test/offer?token=opaque-token"));

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), shareResponse.getStatus());
        assertEquals("kunde@example.com", sentMail.getFirst().document().recipientEmail());
        assertEquals("https://offers.example.test/offer?token=opaque-token", sentMail.getFirst().accessLink());
        assertEquals("OFFER pdf", new String(sentMail.getFirst().document().pdfContent()));
        assertEquals(documentId, repository.findLatestMetadataByReferenceAndOwner(
                DocumentType.OFFER, "angebot-123", "handwerker-42").orElseThrow().documentId());
    }

    @Test
    void createsAndSharesInvoiceWithoutLink() {
        resource.generateInvoiceDocument("rechnung-123", snapshot());

        Response response = resource.shareInvoice("rechnung-123");

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        assertEquals(DocumentType.INVOICE, sentMail.getFirst().document().type());
        assertEquals("kunde@example.com", sentMail.getFirst().document().recipientEmail());
        assertEquals("INVOICE pdf", new String(sentMail.getFirst().document().pdfContent()));
        assertNull(sentMail.getFirst().accessLink());
    }

    @Test
    void createsAndSharesOrderConfirmationWithoutLink() {
        resource.generateOrderConfirmation("auftrag-123", snapshot());

        Response response = resource.shareOrderConfirmation("auftrag-123");

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        assertEquals(DocumentType.ORDER_CONFIRMATION, sentMail.getFirst().document().type());
        assertEquals("kunde@example.com", sentMail.getFirst().document().recipientEmail());
        assertEquals("ORDER_CONFIRMATION pdf", new String(sentMail.getFirst().document().pdfContent()));
        assertNull(sentMail.getFirst().accessLink());
    }

    @Test
    void hidesDocumentsOwnedByAnotherCraftsman() {
        repository.save(new Document(DocumentType.ORDER_CONFIRMATION, "foreign-order", "other-owner",
                "kunde-1", "kunde@example.com", "order.pdf", new byte[] {1}));

        assertThrows(NotFoundException.class, () -> resource.shareOrderConfirmation("foreign-order"));
    }

    @Test
    void rejectsOfferShareWithoutValidAccessLink() {
        assertThrows(BadRequestException.class, () -> resource.shareOffer("missing-link", null));
        assertThrows(BadRequestException.class, () -> resource.shareOffer(
                "invalid-link", new DocumentShareRequest("javascript:alert(1)")));
    }

    private DocumentResource resource(String ownerId) {
        SecurityIdentity identity = mock(SecurityIdentity.class);
        when(identity.getPrincipal()).thenReturn((Principal) () -> ownerId);

        DocumentResource instance = new DocumentResource();
        instance.identity = identity;
        instance.documentPdfGenerator = bean(pdfGenerator);
        instance.repository = bean(repository);
        instance.documentSender = bean(sender);
        return instance;
    }

    @SuppressWarnings("unchecked")
    private static <T> Instance<T> bean(T value) {
        Instance<T> instance = mock(Instance.class);
        when(instance.isResolvable()).thenReturn(true);
        when(instance.get()).thenReturn(value);
        return instance;
    }

    private static DocumentSnapshotRequest snapshot() {
        return new DocumentSnapshotRequest(
                "customer-1",
                LocalDate.of(2026, 11, 15),
                new DocumentSnapshotRequest.Party("Werkmundo Handwerk", "Werkstrasse 1", null),
                new DocumentSnapshotRequest.Party("Max Mustermann", "Kundenweg 2", "kunde@example.com"),
                List.of(new DocumentSnapshotRequest.LineItem(
                        "Reparatur", BigDecimal.valueOf(2), "Stunden", BigDecimal.valueOf(50), BigDecimal.valueOf(100))),
                "EUR",
                BigDecimal.valueOf(100),
                List.of(),
                BigDecimal.valueOf(100),
                "Termin nach Absprache");
    }

    private record SentMail(Document document, String accessLink) {
    }

    private static class InMemoryDocumentRepository implements DocumentRepository {
        private final Map<String, Document> documents = new HashMap<>();

        @Override
        public String save(Document document) {
            String id = UUID.randomUUID().toString();
            documents.put(id, document);
            return id;
        }

        @Override
        public Optional<DocumentMetadata> findMetadataByIdAndOwner(String documentId, String ownerId) {
            return findByIdAndOwner(documentId, ownerId)
                    .map(document -> DocumentMetadata.from(documentId, document));
        }

        @Override
        public List<DocumentMetadata> findMetadataByOwner(String ownerId) {
            return documents.entrySet().stream()
                    .filter(entry -> ownerId.equals(entry.getValue().ownerId()))
                    .map(entry -> DocumentMetadata.from(entry.getKey(), entry.getValue()))
                    .toList();
        }

        @Override
        public Optional<DocumentMetadata> findLatestMetadataByReferenceAndOwner(
                DocumentType type, String referenceId, String ownerId) {
            return documents.entrySet().stream()
                    .filter(entry -> ownerId.equals(entry.getValue().ownerId())
                            && type == entry.getValue().type()
                            && referenceId.equals(entry.getValue().referenceId()))
                    .reduce((older, newer) -> newer)
                    .map(entry -> DocumentMetadata.from(entry.getKey(), entry.getValue()));
        }

        @Override
        public Optional<Document> findByIdAndOwner(String documentId, String ownerId) {
            return Optional.ofNullable(documents.get(documentId))
                    .filter(document -> ownerId.equals(document.ownerId()));
        }
    }
}
