package de.werkmundo.invoice.document.api;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import de.werkmundo.invoice.document.application.port.DocumentSender;
import de.werkmundo.invoice.document.application.port.DocumentPdfGenerator;
import de.werkmundo.invoice.document.application.port.InvoicePdfGenerator;
import de.werkmundo.invoice.document.domain.DocumentRepository;
import de.werkmundo.invoice.document.domain.Document;
import de.werkmundo.invoice.document.domain.DocumentSnapshot;
import de.werkmundo.invoice.document.domain.DocumentType;
import de.werkmundo.invoice.document.domain.DocumentMetadata;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import io.quarkus.arc.lookup.LookupIfProperty;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DocumentResourceTest {
    private static final byte[] TEST_PDF = {1, 2, 3};
    private static final String METADATA_ROUTE = "/documents/{documentId}";

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
                        "angebot-business-123.pdf", TEST_PDF));

        given()
                .when().get(METADATA_ROUTE, documentId)
                .then().statusCode(404);
    }

    @Test
    @TestSecurity(user = "handwerker-42")
    void readsAndSharesStoredOfferDocument() {
        String documentId = repository.get().save(new Document(DocumentType.OFFER, "stored-offer",
                "handwerker-42", "kunde-1", null, "angebot-stored-offer.pdf", TEST_PDF));

        given().when().get("/documents/offers/stored-offer")
                .then().statusCode(200)
                .body("documentId", equalTo(documentId))
                .body("referenceId", equalTo("stored-offer"))
                .body("type", equalTo("OFFER"))
                .body("fileName", equalTo("angebot-stored-offer.pdf"));
        given().when().get(METADATA_ROUTE + "/pdf", documentId)
                .then().statusCode(200).contentType("application/pdf");
        given().when().post("/documents/offers/stored-offer/share").then().statusCode(204);
        assertEquals("kunde-1", adapters.lastSent().customerId());
    }

    @Test
    @TestSecurity(user = "handwerker-42")
    void createsAndDownloadsOfferDocument() {
        String id = given().contentType("application/json")
                .body("""
                        {
                          "snapshot": {
                            "documentDate": "2026-11-23",
                            "issuer": {"name": "Werkmundo"},
                            "customer": {"name": "Kunde"},
                            "items": [{
                              "description": "Material",
                              "quantity": 1,
                              "unit": "Stk.",
                              "unitPrice": 10,
                              "lineTotal": 10
                            }],
                            "netAmount": 10,
                            "taxLines": [],
                            "totalAmount": 11.9,
                            "currency": "EUR"
                          },
                          "customerId": "kunde-1",
                          "recipientEmail": "kunde@example.com"
                        }
                        """)
                .when().post("/documents/offers/offer-123/generate")
                .then().statusCode(201).extract().path("documentId");

        given().when().get(METADATA_ROUTE, id).then().statusCode(200)
                .body("type", equalTo("OFFER"))
                .body("referenceId", equalTo("offer-123"));
        byte[] pdf = given().when().get(METADATA_ROUTE + "/pdf", id)
                .then().statusCode(200).contentType("application/pdf").extract().asByteArray();
        assertArrayEquals(TEST_PDF, pdf);
    }

    @Test
    @TestSecurity(user = "handwerker-42")
    void previewsOfferWithoutSavingIt() {
        int documentCount = repository.get().findMetadataByOwner("handwerker-42").size();
        byte[] pdf = given().contentType("application/json")
                .body("""
                        {
                          "snapshot": {
                            "documentDate": "2026-11-23",
                            "issuer": {"name": "Werkmundo"},
                            "customer": {"name": "Kunde"},
                            "items": [],
                            "netAmount": 0,
                            "taxLines": [],
                            "totalAmount": 0,
                            "currency": "EUR",
                            "notes": "ENTWURF: Keine Angebotspositionen vorhanden."
                          }
                        }
                        """)
                .when().post("/documents/offers/offer-empty/preview")
                .then().statusCode(200).contentType("application/pdf")
                .header("Content-Disposition", startsWith("inline; filename="))
                .extract().asByteArray();

        assertArrayEquals(TEST_PDF, pdf);
        assertEquals(documentCount, repository.get().findMetadataByOwner("handwerker-42").size());
    }

    @Test
    @TestSecurity(user = "handwerker-42")
    void doesNotPersistOfferWithoutPositions() {
        given().contentType("application/json")
                .body("""
                        {
                          "snapshot": {
                            "documentDate": "2026-11-23",
                            "issuer": {"name": "Werkmundo"},
                            "customer": {"name": "Kunde"},
                            "items": [],
                            "netAmount": 0,
                            "taxLines": [],
                            "totalAmount": 0,
                            "currency": "EUR",
                            "notes": "ENTWURF: Keine Angebotspositionen vorhanden."
                          },
                          "customerId": "kunde-1",
                          "recipientEmail": "kunde@example.com"
                        }
                        """)
                .when().post("/documents/offers/offer-empty/generate")
                .then().statusCode(400);
    }

    @Test
    @TestSecurity(user = "handwerker-99")
    void hidesForeignDocumentsFromDownloadListingAndSharing() {
        String id = repository.get().save(new Document(DocumentType.OFFER, "foreign-offer", "handwerker-42",
                "kunde-1", null, "angebot-foreign-offer.pdf", TEST_PDF));
        given().when().get(METADATA_ROUTE + "/pdf", id).then().statusCode(404);
        given().when().post("/documents/offers/foreign-offer/share").then().statusCode(404);
        given().when().get("/documents").then().statusCode(200).body("size()", equalTo(0));
    }

    @Test
    @TestSecurity(user = "handwerker-42")
    void createsInvoiceFromArchivePayloadAndSharesIt() {
        String id = given().contentType("application/json")
                .body("""
                        {"rechnungsentwurf":{"kundendaten":{"email":"kunde@example.com"}}}
                        """)
                .when().post("/documents/invoices/rechnung-123/generate")
                .then().statusCode(201).extract().path("documentId");
        given().when().get(METADATA_ROUTE, id).then().statusCode(200)
                .body("type", equalTo("INVOICE"))
                .body("referenceId", equalTo("rechnung-123"));
        byte[] pdf = given().when().get(METADATA_ROUTE + "/pdf", id)
                .then().statusCode(200).contentType("application/pdf")
                .header("Content-Disposition", startsWith("attachment; filename="))
                .extract().asByteArray();
        assertArrayEquals(TEST_PDF, pdf);
        given().when().get(METADATA_ROUTE + "/pdf?inline=true", id)
                .then().statusCode(200).contentType("application/pdf")
                .header("Content-Disposition", startsWith("inline; filename="));
        given().when().post("/documents/invoices/rechnung-123/share").then().statusCode(204);
        assertEquals("kunde@example.com", adapters.lastSent().recipientEmail());
    }

    @Test
    @TestSecurity(user = "handwerker-42")
    void rejectsInvoiceWithoutCustomerSnapshot() {
        given().contentType("application/json").body("{\"rechnungsentwurf\":{}}")
                .when().post("/documents/invoices/rechnung-123/generate")
                .then().statusCode(400);
    }

    @ApplicationScoped
    static class TestAdapters {
        private final AtomicReference<Document> lastSent = new AtomicReference<>();

        Document lastSent() {
            return lastSent.get();
        }

        @Produces
        @Alternative
        @Priority(1)
        InvoicePdfGenerator invoicePdfGenerator() {
            return draft -> TEST_PDF;
        }

        @Produces
        @Alternative
        @Priority(1)
        DocumentPdfGenerator documentPdfGenerator() {
            return (type, snapshot) -> TEST_PDF;
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
