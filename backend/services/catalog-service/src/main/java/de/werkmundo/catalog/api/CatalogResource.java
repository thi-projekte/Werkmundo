package de.werkmundo.catalog.api;

import de.werkmundo.catalog.application.ManageMaterialUseCase;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.util.Map;

@Authenticated
@Path("/catalog/material")
@Produces(MediaType.APPLICATION_JSON)
public class CatalogResource {
    private final ManageMaterialUseCase catalog;
    private final SecurityIdentity identity;

    public CatalogResource(ManageMaterialUseCase catalog, SecurityIdentity identity) {
        this.catalog = catalog;
        this.identity = identity;
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
}
