package de.werkmundo.document.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.werkmundo.document.application.port.DocumentSender;
import de.werkmundo.document.domain.Document;
import de.werkmundo.document.domain.DocumentMetadata;
import de.werkmundo.document.domain.DocumentRepository;
import de.werkmundo.document.domain.DocumentType;
import io.quarkus.arc.lookup.LookupIfProperty;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DocumentResourceTest {
    private static final String METADATA_ROUTE = "/documents/{documentId}";
    private static final String SNAPSHOT = """
            {
              "customerId": "customer-1",
              "documentDate": "2026-11-15",
              "issuer": {"name": "Werkmundo Handwerk", "address": "Werkstrasse 1, 85049 Ingolstadt"},
              "customer": {"name": "Max Mustermann", "address": "Kundenweg 2, 85051 Ingolstadt",
                           "email": "kunde@example.com"},
              "items": [
                {"description": "Reparatur", "quantity": 2, "unit": "Stunden",
                 "unitPrice": 50.00, "lineTotal": 100.00}
              ],
              "currency": "EUR",
              "netAmount": 100.00,
              "taxLines": [],
              "totalAmount": 100.00,
              "notes": "Termin nach Absprache"
            }
            """;

    @Inject
    Instance<DocumentRepository> repository;

    @Inject
    TestAdapters adapters;

    @Test
    void requiresAuthentication() {
        given()
                .when().get(METADATA_ROUTE, "unknown")
                .then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "handwerker-42")
    void returnsNotFoundForUnknownDocument() {
        given()
                .when().get(METADATA_ROUTE, "unknown")
                .then().statusCode(404);
    }

    @Test
    @TestSecurity(user = "handwerker-99")
    void returnsNotFoundForAnotherOwnersDocument() {
        String documentId = repository.get().save(
                new Document(DocumentType.OFFER, "business-123", "handwerker-42", "kunde-1", null,
                        "angebot-business-123.pdf", new byte[] {1}));

        given()
                .when().get(METADATA_ROUTE, documentId)
                .then().statusCode(404);
    }

    @Test
    @TestSecurity(user = "handwerker-42")
    void readsAndSharesStoredOfferDocument() {
        String documentId = repository.get().save(new Document(DocumentType.OFFER, "stored-offer",
                "handwerker-42", "kunde-1", null, "angebot-stored-offer.pdf", new byte[] {1}));

        given().when().get("/documents/offers/stored-offer")
                .then().statusCode(200)
                .body("documentId", equalTo(documentId))
                .body("referenceId", equalTo("stored-offer"))
                .body("type", equalTo("OFFER"))
                .body("fileName", equalTo("angebot-stored-offer.pdf"));
        given().when().get(METADATA_ROUTE + "/pdf", documentId).then().statusCode(404);
        given().when().post("/documents/offers/stored-offer/share").then().statusCode(204);
        assertEquals("kunde-1", adapters.lastSent().customerId());
    }

    @Test
    @TestSecurity(user = "handwerker-99")
    void hidesForeignDocumentsFromDownloadListingAndSharing() {
        String id = repository.get().save(new Document(DocumentType.OFFER, "foreign-offer", "handwerker-42",
                "kunde-1", null, "angebot-foreign-offer.pdf", new byte[] {1}));
        given().when().get(METADATA_ROUTE + "/pdf", id).then().statusCode(404);
        given().when().post("/documents/offers/foreign-offer/share").then().statusCode(404);
        given().when().get("/documents").then().statusCode(200).body("size()", equalTo(0));
    }

    @Test
    @TestSecurity(user = "handwerker-42")
    void createsInvoicePdfFromSnapshotAndSharesIt() throws Exception {
        String id = given().contentType("application/json")
                .body(SNAPSHOT)
                .when().post("/documents/invoices/rechnung-123/generate")
                .then().statusCode(201).extract().path("documentId");

        given().when().get(METADATA_ROUTE, id).then().statusCode(200)
                .body("type", equalTo("INVOICE"))
                .body("referenceId", equalTo("rechnung-123"));
        byte[] pdf = given().when().get(METADATA_ROUTE + "/pdf", id)
                .then().statusCode(200).contentType("application/pdf").extract().asByteArray();
        String text = extractText(pdf);
        assertTrue(text.contains("Invoice"));
        assertTrue(text.contains("Max Mustermann"));
        assertTrue(text.contains("100.00 EUR"));

        given().when().post("/documents/invoices/rechnung-123/share").then().statusCode(204);
        assertEquals("kunde@example.com", adapters.lastSent().recipientEmail());
    }

    @Test
    @TestSecurity(user = "handwerker-42")
    void createsOrderConfirmationPdfFromSnapshot() throws Exception {
        String id = given().contentType("application/json")
                .body(SNAPSHOT)
                .when().post("/documents/order-confirmations/order-123/generate")
                .then().statusCode(201).extract().path("documentId");

        given().when().get(METADATA_ROUTE, id).then().statusCode(200)
                .body("type", equalTo("ORDER_CONFIRMATION"))
                .body("fileName", equalTo("auftragsbestaetigung-order-123.pdf"));
        byte[] pdf = given().when().get(METADATA_ROUTE + "/pdf", id)
                .then().statusCode(200).contentType("application/pdf").extract().asByteArray();
        String text = extractText(pdf);
        assertTrue(text.contains("Order confirmation"));
        assertTrue(text.contains("Reparatur"));
        assertTrue(text.contains("Termin nach Absprache"));
    }

    @Test
    @TestSecurity(user = "handwerker-42")
    void rejectsSnapshotWithoutRequiredFields() {
        given().contentType("application/json").body("""
                        {"documentDate":"2026-11-15","issuer":{"name":"Handwerker"},
                         "items":[{"description":"Leistung","quantity":1,"unitPrice":10,"lineTotal":10}],
                         "currency":"EUR","netAmount":10,"taxLines":[],"totalAmount":10}
                        """)
                .when().post("/documents/invoices/rechnung-123/generate")
                .then().statusCode(400);

        given().contentType("application/json").body(SNAPSHOT.replace("\"taxLines\": [],", ""))
                .when().post("/documents/invoices/rechnung-123/generate")
                .then().statusCode(400);
    }

    private static String extractText(byte[] pdfBytes) throws Exception {
        try (var pdf = Loader.loadPDF(pdfBytes)) {
            return new PDFTextStripper().getText(pdf);
        }
    }

    @ApplicationScoped
    static class TestAdapters {
        private final AtomicReference<Document> lastSent = new AtomicReference<>();

        Document lastSent() {
            return lastSent.get();
        }

        @Produces
        DocumentSender documentSender() {
            return lastSent::set;
        }

        @Produces
        @ApplicationScoped
        @LookupIfProperty(name = "document.storage.postgres.enabled", stringValue = "false", lookupIfMissing = true)
        DocumentRepository repository() {
            return new DocumentRepository() {
                private final Map<String, Document> documents = new LinkedHashMap<>();

                @Override
                public synchronized String save(Document document) {
                    String id = UUID.randomUUID().toString();
                    documents.put(id, document);
                    return id;
                }

                @Override
                public synchronized Optional<Document> findByIdAndOwner(String documentId, String ownerId) {
                    return Optional.ofNullable(documents.get(documentId))
                            .filter(document -> ownerId.equals(document.ownerId()));
                }

                @Override
                public Optional<DocumentMetadata> findMetadataByIdAndOwner(String documentId, String ownerId) {
                    return findByIdAndOwner(documentId, ownerId)
                            .map(document -> DocumentMetadata.from(documentId, document));
                }

                @Override
                public synchronized List<DocumentMetadata> findMetadataByOwner(String ownerId) {
                    return documents.entrySet().stream()
                            .filter(entry -> ownerId.equals(entry.getValue().ownerId()))
                            .map(entry -> DocumentMetadata.from(entry.getKey(), entry.getValue()))
                            .toList();
                }

                @Override
                public synchronized Optional<DocumentMetadata> findLatestMetadataByReferenceAndOwner(
                        DocumentType type, String referenceId, String ownerId) {
                    return documents.entrySet().stream()
                            .filter(entry -> {
                                Document document = entry.getValue();
                                return ownerId.equals(document.ownerId()) && type == document.type()
                                        && referenceId.equals(document.referenceId());
                            })
                            .reduce((older, newer) -> newer)
                            .map(entry -> DocumentMetadata.from(entry.getKey(), entry.getValue()));
                }
            };
        }
    }
}
