package de.werkmundo.offer.api;

import de.werkmundo.offer.api.dto.ApproveOfferResponse;
import de.werkmundo.offer.api.dto.CreateOfferRequest;
import de.werkmundo.offer.api.dto.OfferResponse;
import de.werkmundo.offer.api.dto.UpdateOfferRequest;
import de.werkmundo.offer.application.usecase.offer.ApproveOfferResult;
import de.werkmundo.offer.application.usecase.offer.ApproveOfferUseCase;
import de.werkmundo.offer.application.usecase.offer.CancelOfferUseCase;
import de.werkmundo.offer.application.usecase.offer.CreateOfferUseCase;
import de.werkmundo.offer.application.usecase.offer.GetOfferUseCase;
import de.werkmundo.offer.application.usecase.offer.ListOffersUseCase;
import de.werkmundo.offer.application.usecase.offer.MarkOfferSentUseCase;
import de.werkmundo.offer.application.usecase.offer.OfferSortOrder;
import de.werkmundo.offer.application.usecase.offer.UpdateOfferUseCase;
import de.werkmundo.offer.domain.Offer;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriBuilder;
import java.security.Principal;
import java.util.List;

@Path("/offers")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OfferResource {
    private final CreateOfferUseCase createOfferUseCase;
    private final GetOfferUseCase getOfferUseCase;
    private final ListOffersUseCase listOffersUseCase;
    private final UpdateOfferUseCase updateOfferUseCase;
    private final ApproveOfferUseCase approveOfferUseCase;
    private final MarkOfferSentUseCase markOfferSentUseCase;
    private final CancelOfferUseCase cancelOfferUseCase;

    @Inject
    public OfferResource(
            CreateOfferUseCase createOfferUseCase,
            GetOfferUseCase getOfferUseCase,
            ListOffersUseCase listOffersUseCase,
            UpdateOfferUseCase updateOfferUseCase,
            ApproveOfferUseCase approveOfferUseCase,
            MarkOfferSentUseCase markOfferSentUseCase,
            CancelOfferUseCase cancelOfferUseCase) {
        this.createOfferUseCase = createOfferUseCase;
        this.getOfferUseCase = getOfferUseCase;
        this.listOffersUseCase = listOffersUseCase;
        this.updateOfferUseCase = updateOfferUseCase;
        this.approveOfferUseCase = approveOfferUseCase;
        this.markOfferSentUseCase = markOfferSentUseCase;
        this.cancelOfferUseCase = cancelOfferUseCase;
    }

    @POST
    public Response create(CreateOfferRequest request, @Context SecurityContext securityContext) {
        if (request == null) {
            throw new BadRequestException("Request body is required");
        }

        Offer offer = createOfferUseCase.execute(request.toCommand(requireCraftsmanId(securityContext)));
        return Response.created(UriBuilder.fromPath("/offers/{businessKey}")
                        .build(offer.getBusinessKey()))
                .entity(OfferResponse.from(offer))
                .build();
    }

    @GET
    public List<OfferResponse> list(
            @QueryParam("sort") String sort,
            @Context SecurityContext securityContext) {
        OfferSortOrder sortOrder = parseSortOrder(sort);
        return listOffersUseCase.execute(requireCraftsmanId(securityContext), sortOrder).stream()
                .map(OfferResponse::from)
                .toList();
    }

    @GET
    @Path("/{businessKey}")
    public OfferResponse get(
            @PathParam("businessKey") String businessKey,
            @Context SecurityContext securityContext) {
        return OfferResponse.from(getOfferUseCase.execute(businessKey, requireCraftsmanId(securityContext)));
    }

    @PUT
    @Path("/{businessKey}")
    public OfferResponse update(
            @PathParam("businessKey") String businessKey,
            UpdateOfferRequest request,
            @Context SecurityContext securityContext) {
        if (request == null) {
            throw new BadRequestException("Request body is required");
        }

        String craftsmanId = requireCraftsmanId(securityContext);
        getOfferUseCase.execute(businessKey, craftsmanId);
        return OfferResponse.from(updateOfferUseCase.execute(request.toCommand(businessKey, craftsmanId)));
    }

    @POST
    @Path("/{businessKey}/approve")
    public ApproveOfferResponse approve(
            @PathParam("businessKey") String businessKey,
            @Context SecurityContext securityContext) {
        getOfferUseCase.execute(businessKey, requireCraftsmanId(securityContext));
        ApproveOfferResult result = approveOfferUseCase.execute(businessKey);
        return ApproveOfferResponse.from(result);
    }

    @POST
    @Path("/{businessKey}/sent")
    public OfferResponse markSent(
            @PathParam("businessKey") String businessKey,
            @Context SecurityContext securityContext) {
        getOfferUseCase.execute(businessKey, requireCraftsmanId(securityContext));
        return OfferResponse.from(markOfferSentUseCase.execute(businessKey));
    }

    @POST
    @Path("/{businessKey}/cancel")
    public OfferResponse cancel(
            @PathParam("businessKey") String businessKey,
            @Context SecurityContext securityContext) {
        getOfferUseCase.execute(businessKey, requireCraftsmanId(securityContext));
        return OfferResponse.from(cancelOfferUseCase.execute(businessKey));
    }

    private String requireCraftsmanId(SecurityContext securityContext) {
        Principal principal = securityContext.getUserPrincipal();
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new NotAuthorizedException("Authentication is required");
        }
        return principal.getName();
    }

    private OfferSortOrder parseSortOrder(String sort) {
        if (sort == null || sort.isBlank()) {
            return OfferSortOrder.NEWEST_FIRST;
        }
        try {
            return OfferSortOrder.valueOf(sort);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Unsupported sort order", exception);
        }
    }
}
