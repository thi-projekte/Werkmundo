package de.werkmundo.invoice.document.api;

import com.fasterxml.jackson.databind.JsonNode;
import de.werkmundo.invoice.document.application.port.DocumentPdfGenerator;
import de.werkmundo.invoice.document.application.port.DocumentSender;
import de.werkmundo.invoice.document.application.port.InvoicePdfGenerator;
import de.werkmundo.invoice.document.application.usecase.CreateOfferDocumentUseCase;
import de.werkmundo.invoice.document.application.usecase.CreateInvoiceDocumentUseCase;
import de.werkmundo.invoice.document.domain.Document;
import de.werkmundo.invoice.document.domain.DocumentMetadata;
import de.werkmundo.invoice.document.domain.DocumentRepository;
import de.werkmundo.invoice.document.domain.DocumentSnapshot;
import de.werkmundo.invoice.document.domain.DocumentType;
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
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.ServiceUnavailableException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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
    Instance<DocumentPdfGenerator> documentPdfGenerator;

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

    @POST
    @Path("/offers/{businessKey}/generate")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response generateOfferDocument(
            @PathParam("businessKey") String businessKey, GenerateOfferRequest request) {
        requireReference(businessKey);
        if (request == null || request.snapshot() == null || request.snapshot().items().isEmpty()) {
            throw new BadRequestException("Offer snapshot with positions is required");
        }
        if (request.recipientEmail() == null || request.recipientEmail().isBlank()) {
            throw new BadRequestException("Offer customer email is required");
        }
        if (!documentPdfGenerator.isResolvable()) {
            throw new ServiceUnavailableException("Document PDF generation is not configured");
        }

        CreateOfferDocumentUseCase useCase =
                new CreateOfferDocumentUseCase(documentPdfGenerator.get(), documentRepository());
        return created(useCase.execute(
                businessKey, ownerId(), request.customerId(), request.recipientEmail(), request.snapshot()));
    }

    @POST
    @Path("/offers/{businessKey}/preview")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(APPLICATION_PDF)
    public Response previewOfferDocument(
            @PathParam("businessKey") String businessKey, OfferPreviewRequest request) {
        requireReference(businessKey);
        if (request == null || request.snapshot() == null) {
            throw new BadRequestException("Offer snapshot is required");
        }
        if (!documentPdfGenerator.isResolvable()) {
            throw new ServiceUnavailableException("Document PDF generation is not configured");
        }

        byte[] pdf = documentPdfGenerator.get().generate(DocumentType.OFFER, request.snapshot());
        if (pdf == null || pdf.length == 0) {
            throw new IllegalStateException("Offer PDF generator returned no content");
        }
        String fileName = DocumentType.OFFER.fileName(businessKey).replaceAll("[^A-Za-z0-9_.-]", "_");
        return Response.ok(pdf, APPLICATION_PDF)
                .header("Content-Disposition", "inline; filename=\"" + fileName + "\"")
                .header("Cache-Control", "private, no-store")
                .build();
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

    @GET
    @Path("/{documentId}/pdf")
    @Produces(APPLICATION_PDF)
    public Response downloadDocumentPdf(
            @PathParam("documentId") String documentId,
            @QueryParam("inline") boolean inline) {
        requireReference(documentId);
        Document document = findDocument(documentId);
        return pdfResponse(document, inline);
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

    private static Response pdfResponse(Document document, boolean inline) {
        String fileName = document.fileName().replaceAll("[^A-Za-z0-9_.-]", "_");
        return Response.ok(document.pdfContent(), APPLICATION_PDF)
                .header("Content-Disposition",
                        (inline ? "inline" : "attachment") + "; filename=\"" + fileName + "\"")
                .header("Cache-Control", "private, no-store")
                .build();
    }

    public record CreatedDocument(String documentId) {
    }

    public record GenerateInvoiceRequest(JsonNode rechnungsentwurf) {
    }

    public record GenerateOfferRequest(
            DocumentSnapshot snapshot,
            String customerId,
            String recipientEmail) {
    }

    public record OfferPreviewRequest(DocumentSnapshot snapshot) {
    }
}
