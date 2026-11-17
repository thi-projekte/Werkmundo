package de.werkmundo.document.api;

import de.werkmundo.document.application.port.DocumentSender;
import de.werkmundo.document.application.port.DocumentPdfGenerator;
import de.werkmundo.document.application.usecase.CreateDocumentFromSnapshotUseCase;
import de.werkmundo.document.api.dto.DocumentShareRequest;
import de.werkmundo.document.api.dto.DocumentSnapshotRequest;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Path("/documents")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class DocumentResource {
    private static final String APPLICATION_PDF = "application/pdf";

    @Inject
    SecurityIdentity identity;

    @Inject
    Instance<DocumentPdfGenerator> documentPdfGenerator;

    @Inject
    Instance<DocumentRepository> repository;

    @Inject
    Instance<DocumentSender> documentSender;

    @POST
    @Path("/offers/{businessKey}/generate")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response generateOfferDocument(
            @PathParam("businessKey") String businessKey, DocumentSnapshotRequest request) {
        return createDocument(DocumentType.OFFER, businessKey, request);
    }

    @POST
    @Path("/invoices/{businessKey}/generate")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response generateInvoiceDocument(
            @PathParam("businessKey") String businessKey, DocumentSnapshotRequest request) {
        return createDocument(DocumentType.INVOICE, businessKey, request);
    }

    @POST
    @Path("/order-confirmations/{businessKey}/generate")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response generateOrderConfirmation(
            @PathParam("businessKey") String businessKey, DocumentSnapshotRequest request) {
        return createDocument(DocumentType.ORDER_CONFIRMATION, businessKey, request);
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
    public Response downloadPdf(@PathParam("documentId") String documentId) {
        requireReference(documentId);
        Document document = findDocument(documentId);
        if (document.type() == DocumentType.OFFER) {
            throw new NotFoundException("Generated document was not found");
        }
        return pdfResponse(document);
    }

    @POST
    @Path("/offers/{businessKey}/share")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response shareOffer(
            @PathParam("businessKey") String businessKey, DocumentShareRequest request) {
        return share(DocumentType.OFFER, businessKey, requireOfferAccessLink(request));
    }

    @POST
    @Path("/invoices/{businessKey}/share")
    public Response shareInvoice(@PathParam("businessKey") String businessKey) {
        return share(DocumentType.INVOICE, businessKey, null);
    }

    @POST
    @Path("/order-confirmations/{businessKey}/share")
    public Response shareOrderConfirmation(@PathParam("businessKey") String businessKey) {
        return share(DocumentType.ORDER_CONFIRMATION, businessKey, null);
    }

    private Response share(DocumentType type, String referenceId, String accessLink) {
        Document document = findDocument(findByReference(type, referenceId).documentId());
        if (document.recipientEmail() == null || document.recipientEmail().isBlank()) {
            throw new WebApplicationException("Document has no recipient email", Response.Status.CONFLICT);
        }
        if (!documentSender.isResolvable()) {
            throw new ServiceUnavailableException("Document sending is not configured");
        }

        documentSender.get().sendToCustomer(document, accessLink);
        return Response.noContent().build();
    }

    private static String requireOfferAccessLink(DocumentShareRequest request) {
        if (request == null || request.accessLink() == null || request.accessLink().isBlank()) {
            throw new BadRequestException("accessLink is required for offer sharing");
        }
        try {
            URI link = new URI(request.accessLink());
            if (!link.isAbsolute() || link.getHost() == null
                    || !("https".equalsIgnoreCase(link.getScheme())
                            || "http".equalsIgnoreCase(link.getScheme()))
                    || link.getUserInfo() != null) {
                throw new BadRequestException("accessLink must be an absolute HTTP(S) URL");
            }
        } catch (URISyntaxException exception) {
            throw new BadRequestException("accessLink must be a valid HTTP(S) URL", exception);
        }
        return request.accessLink();
    }

    private Response createDocument(DocumentType type, String businessKey, DocumentSnapshotRequest request) {
        requireReference(businessKey);
        validateSnapshot(request);
        if (!documentPdfGenerator.isResolvable()) {
            throw new ServiceUnavailableException("Document PDF generation is not configured");
        }

        CreateDocumentFromSnapshotUseCase useCase =
                new CreateDocumentFromSnapshotUseCase(documentPdfGenerator.get(), documentRepository());
        return created(useCase.execute(type, businessKey, ownerId(), request));
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

    private static void validateSnapshot(DocumentSnapshotRequest request) {
        if (request == null) {
            throw new BadRequestException("Document snapshot is required");
        }
        if (request.documentDate() == null) {
            throw new BadRequestException("documentDate is required");
        }
        validateParty(request.issuer(), "issuer");
        validateParty(request.customer(), "customer");
        if (request.customer().email() == null || request.customer().email().isBlank()) {
            throw new BadRequestException("customer.email is required");
        }
        if (request.items() == null || request.items().isEmpty()) {
            throw new BadRequestException("items must contain at least one entry");
        }
        for (DocumentSnapshotRequest.LineItem item : request.items()) {
            if (item == null || item.description() == null || item.description().isBlank()
                    || item.quantity() == null || item.quantity().signum() <= 0
                    || item.unitPrice() == null || item.unitPrice().signum() < 0
                    || item.lineTotal() == null || item.lineTotal().signum() < 0) {
                throw new BadRequestException(
                        "Each item requires a description, positive quantity, and non-negative prices");
            }
        }
        if (request.currency() == null || !request.currency().matches("[A-Z]{3}")) {
            throw new BadRequestException("currency must be a three-letter uppercase code");
        }
        if (request.netAmount() == null || request.netAmount().signum() < 0
                || request.totalAmount() == null || request.totalAmount().signum() < 0) {
            throw new BadRequestException("netAmount and totalAmount must be non-negative");
        }
        if (request.taxLines() == null) {
            throw new BadRequestException("taxLines is required; use an empty list when no tax breakdown is supplied");
        }
        for (DocumentSnapshotRequest.TaxLine taxLine : request.taxLines()) {
            if (taxLine == null || taxLine.label() == null || taxLine.label().isBlank()
                    || taxLine.amount() == null || taxLine.amount().signum() < 0
                    || (taxLine.rate() != null && taxLine.rate().signum() < 0)) {
                throw new BadRequestException("Each tax line requires a label and non-negative amounts");
            }
        }
    }

    private static void validateParty(DocumentSnapshotRequest.Party party, String name) {
        if (party == null || party.name() == null || party.name().isBlank()) {
            throw new BadRequestException(name + ".name is required");
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
}
