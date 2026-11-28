package de.werkmundo.catalog.api;

import de.werkmundo.catalog.application.ManageMaterialUseCase;
import de.werkmundo.catalog.domain.CatalogItem;
import de.werkmundo.catalog.domain.CatalogReader;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Authenticated
@Path("/catalog/material")
@Produces(MediaType.APPLICATION_JSON)
public class CatalogResource {
    private final ManageMaterialUseCase catalog;
    private final SecurityIdentity identity;
    private final CatalogReader reader;

    public CatalogResource(ManageMaterialUseCase catalog, SecurityIdentity identity, CatalogReader reader) {
        this.catalog = catalog;
        this.identity = identity;
        this.reader = reader;
    }

    @POST
    @Path("/import/csv")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response importCsv(@RestForm("file") FileUpload file) {
        if (file == null) {
            return Response.status(400).entity(Map.of("message", "Upload-Feld file fehlt")).build();
        }
        try {
            return Response.ok(catalog.importCsv(identity.getPrincipal().getName(), file.uploadedFile())).build();
        } catch (ManageMaterialUseCase.InvalidCsvException e) {
            return Response.status(400).entity(Map.of("message", e.getMessage())).build();
        }
    }

    @GET
    @Path("/status")
    public Map<String, Long> status() {
        return Map.of("articleCount", reader.count(identity.getPrincipal().getName()));
    }

    @GET
    @Path("/search")
    public Map<String, List<Candidate>> search(@QueryParam("q") String query,
            @QueryParam("limit") @DefaultValue("15") int limit,
            @HeaderParam("X-Handwerker-Id") String handwerkerId) {
        String owner = owner(handwerkerId);
        if (limit < 1 || limit > 100) throw new BadRequestException("limit muss zwischen 1 und 100 liegen");
        if (query == null || query.isBlank()) return Map.of("candidates", List.of());
        return Map.of("candidates", reader.search(owner, query.trim(), limit).stream().map(Candidate::new).toList());
    }

    @GET
    @Path("/{id}")
    public CatalogItem detail(@PathParam("id") UUID id,
            @HeaderParam("X-Handwerker-Id") String handwerkerId) {
        return reader.find(owner(handwerkerId), id).orElseThrow(NotFoundException::new);
    }

    private String owner(String handwerkerId) {
        if (!identity.hasRole("process-engine")) return identity.getPrincipal().getName();
        if (handwerkerId == null || handwerkerId.isBlank()) {
            throw new BadRequestException("X-Handwerker-Id fehlt für technischen Katalogzugriff");
        }
        return handwerkerId;
    }

    // Search candidates go to the AI service; prices are only available from the detail endpoint.
    public record Candidate(UUID id, String articleNumber, String name, String manufacturer,
                            String description, String category, String unit) {
        Candidate(CatalogItem item) {
            this(item.id(), item.articleNumber(), item.name(), item.manufacturer(),
                    item.description(), item.category(), item.unit());
        }
    }
}
