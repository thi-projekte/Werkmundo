package de.werkmundo.document.api;

import com.fasterxml.jackson.databind.JsonNode;
import de.werkmundo.document.application.port.DocumentSender;
import de.werkmundo.document.application.port.InvoicePdfGenerator;
import de.werkmundo.document.application.port.OfferPdfGenerator;
import de.werkmundo.document.application.usecase.CreateInvoiceDocumentUseCase;
import de.werkmundo.document.domain.Document;
import de.werkmundo.document.domain.DocumentMetadata;
import de.werkmundo.document.domain.DocumentRepository;
import de.werkmundo.document.domain.DocumentType;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.ServiceUnavailableException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.List;

@Path("/documents")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class DocumentResource {
    private static final String APPLICATION_PDF = "application/pdf";

    @Inject
    SecurityIdentity identity;

    @Inject
    Instance<InvoicePdfGenerator> invoicePdfGenerator;

    @Inject
    Instance<OfferPdfGenerator> offerPdfGenerator;

    @Inject
    Instance<DocumentRepository> repository;

    @Inject
    Instance<DocumentSender> documentSender;

    @POST
    @Path("/invoices/{businessKey}/generate")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response generateInvoiceDocument(
            @PathParam("businessKey") String businessKey, GenerateInvoiceRequest request) {
        requireReference(businessKey);
        if (request == null || request.rechnungsentwurf() == null || !request.rechnungsentwurf().isObject()) {
            throw new BadRequestException("Invoice draft is required");
        }
        String recipientEmail = request.rechnungsentwurf().path("kundendaten").path("email").textValue();
        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new BadRequestException("Invoice draft with customer email is required");
        }
        if (!invoicePdfGenerator.isResolvable()) {
            throw new ServiceUnavailableException("Invoice PDF generation is not configured");
        }

        CreateInvoiceDocumentUseCase useCase =
                new CreateInvoiceDocumentUseCase(invoicePdfGenerator.get(), documentRepository());
        return created(useCase.execute(businessKey, ownerId(), request.rechnungsentwurf(), recipientEmail));
    }

    @GET
    public List<DocumentMetadata> listDocuments() {
        return documentRepository().findMetadataByOwner(ownerId());
    }

    @GET
    @Path("/{documentId}")
    public DocumentMetadata getDocumentMetadata(@PathParam("documentId") String documentId) {
        requireReference(documentId);
        return documentRepository().findMetadataByIdAndOwner(documentId, ownerId())
                .orElseThrow(() -> new NotFoundException("Document was not found"));
    }

    @GET
    @Path("/offers/{businessKey}")
    public DocumentMetadata getOfferDocumentMetadata(@PathParam("businessKey") String businessKey) {
        return findByReference(DocumentType.OFFER, businessKey);
    }

    @POST
    @Path("/offers/{businessKey}/pdf")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(APPLICATION_PDF)
    public Response generateOfferPdf(
            @PathParam("businessKey") String businessKey, JsonNode offerDraft) {
        requireReference(businessKey);
        return offerPdfResponse(offerDraft, false, "angebot-" + businessKey + ".pdf");
    }

    @POST
    @Path("/offers/demo/pdf")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(APPLICATION_PDF)
    public Response generateDemoOfferPdf(JsonNode offerDraft) {
        return offerPdfResponse(offerDraft, true, "werkmundo-demo-angebot.pdf");
    }

    @GET
    @Path("/{documentId}/pdf")
    @Produces(APPLICATION_PDF)
    public Response downloadInvoicePdf(@PathParam("documentId") String documentId) {
        requireReference(documentId);
        Document document = findDocument(documentId);
        if (document.type() != DocumentType.INVOICE) {
            throw new NotFoundException("Invoice document was not found");
        }
        return pdfResponse(document);
    }

    @POST
    @Path("/offers/{businessKey}/share")
    public Response shareOffer(@PathParam("businessKey") String businessKey) {
        return share(DocumentType.OFFER, businessKey);
    }

    @POST
    @Path("/invoices/{businessKey}/share")
    public Response shareInvoice(@PathParam("businessKey") String businessKey) {
        return share(DocumentType.INVOICE, businessKey);
    }

    private Response share(DocumentType type, String referenceId) {
        Document document = findDocument(findByReference(type, referenceId).documentId());
        boolean hasCustomer = document.customerId() != null && !document.customerId().isBlank();
        boolean hasEmail = document.recipientEmail() != null && !document.recipientEmail().isBlank();
        if (!hasCustomer && !hasEmail) {
            throw new WebApplicationException("Document has no recipient", Response.Status.CONFLICT);
        }
        if (!documentSender.isResolvable()) {
            throw new ServiceUnavailableException("Document sending is not configured");
        }

        documentSender.get().sendToCustomer(document);
        return Response.noContent().build();
    }

    private Response offerPdfResponse(JsonNode offerDraft, boolean demo, String fileName) {
        if (offerDraft == null || !offerDraft.isObject()
                || !offerDraft.path("positions").isArray()
                || offerDraft.path("positions").isEmpty()) {
            throw new BadRequestException("Generated offer positions are required");
        }
        if (!offerPdfGenerator.isResolvable()) {
            throw new ServiceUnavailableException("Offer PDF generation is not configured");
        }

        byte[] pdf = offerPdfGenerator.get().generate(offerDraft, demo || hasIncompleteOfferData(offerDraft));
        if (pdf == null || pdf.length == 0) {
            throw new IllegalStateException("Offer PDF generator returned no content");
        }
        return Response.ok(pdf, APPLICATION_PDF)
                .header("Content-Disposition", "inline; filename=\"" + fileName.replaceAll("[^A-Za-z0-9_.-]", "_") + "\"")
                .header("Cache-Control", "private, no-store")
                .build();
    }

    private static boolean hasIncompleteOfferData(JsonNode offer) {
        JsonNode customer = offer.path("customer");
        JsonNode seller = offer.path("seller");
        JsonNode positions = offer.path("positions");
        if (!hasText(customer, "name")
                || !hasText(customer, "email")
                || !hasText(customer, "street")
                || !hasText(customer, "postalCode")
                || !hasText(customer, "city")
                || !hasText(seller, "companyName")
                || !hasText(seller, "email")
                || !hasText(seller, "street")
                || !hasText(seller, "postalCode")
                || !hasText(seller, "city")) {
            return true;
        }

        for (JsonNode position : positions) {
            if (!hasText(position, "bezeichnung")
                    || !hasText(position, "einheit")
                    || !validAmount(position.path("menge"), true)
                    || !validAmount(position.path("einzelPreis"), false)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasText(JsonNode parent, String field) {
        JsonNode value = parent.path(field);
        return !value.isMissingNode() && !value.isNull() && !value.asText().isBlank();
    }

    private static boolean validAmount(JsonNode value, boolean mustBePositive) {
        if (value.isMissingNode() || value.isNull()) return false;
        try {
            BigDecimal amount = new BigDecimal(value.asText());
            return mustBePositive ? amount.signum() > 0 : amount.signum() >= 0;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private DocumentMetadata findByReference(DocumentType type, String referenceId) {
        requireReference(referenceId);
        return documentRepository().findLatestMetadataByReferenceAndOwner(type, referenceId, ownerId())
                .orElseThrow(() -> new NotFoundException("Document was not found"));
    }

    private Document findDocument(String documentId) {
        String ownerId = ownerId();
        return documentRepository().findByIdAndOwner(documentId, ownerId)
                .filter(document -> ownerId.equals(document.ownerId()))
                .orElseThrow(() -> new NotFoundException("Document was not found"));
    }

    private DocumentRepository documentRepository() {
        if (!repository.isResolvable()) {
            throw new ServiceUnavailableException("Document storage is not configured");
        }
        return repository.get();
    }

    private String ownerId() {
        return identity.getPrincipal().getName();
    }

    private static void requireReference(String reference) {
        if (reference.isBlank()) {
            throw new BadRequestException("Document reference is required");
        }
    }

    private static Response created(String documentId) {
        return Response.status(Response.Status.CREATED).entity(new CreatedDocument(documentId)).build();
    }

    private static Response pdfResponse(Document document) {
        String fileName = document.fileName().replaceAll("[^A-Za-z0-9_.-]", "_");
        return Response.ok(document.pdfContent(), APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                .header("Cache-Control", "private, no-store")
                .build();
    }

    public record CreatedDocument(String documentId) {
    }

    public record GenerateInvoiceRequest(JsonNode rechnungsentwurf) {
    }
}
