package de.werkmundo.document.api;

import com.fasterxml.jackson.databind.JsonNode;
import de.werkmundo.document.application.port.DocumentSender;
import de.werkmundo.document.application.port.InvoicePdfGenerator;
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
