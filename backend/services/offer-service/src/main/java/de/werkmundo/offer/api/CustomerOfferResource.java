package de.werkmundo.offer.api;

import de.werkmundo.offer.api.dto.ChangeRequest;
import de.werkmundo.offer.api.dto.OfferResponse;
import de.werkmundo.offer.application.usecase.customer.AcceptOfferUseCase;
import de.werkmundo.offer.application.usecase.customer.GetOfferByAccessTokenUseCase;
import de.werkmundo.offer.application.usecase.customer.RejectOfferUseCase;
import de.werkmundo.offer.application.usecase.customer.RequestOfferChangesUseCase;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/offers/{businessKey}/customer")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CustomerOfferResource {
    private final GetOfferByAccessTokenUseCase getOfferByAccessTokenUseCase;
    private final AcceptOfferUseCase acceptOfferUseCase;
    private final RejectOfferUseCase rejectOfferUseCase;
    private final RequestOfferChangesUseCase requestOfferChangesUseCase;

    @Inject
    public CustomerOfferResource(
            GetOfferByAccessTokenUseCase getOfferByAccessTokenUseCase,
            AcceptOfferUseCase acceptOfferUseCase,
            RejectOfferUseCase rejectOfferUseCase,
            RequestOfferChangesUseCase requestOfferChangesUseCase) {
        this.getOfferByAccessTokenUseCase = getOfferByAccessTokenUseCase;
        this.acceptOfferUseCase = acceptOfferUseCase;
        this.rejectOfferUseCase = rejectOfferUseCase;
        this.requestOfferChangesUseCase = requestOfferChangesUseCase;
    }

    @GET
    public OfferResponse get(
            @PathParam("businessKey") String businessKey,
            @HeaderParam("X-Offer-Access-Token") String accessToken) {
        return OfferResponse.from(getOfferByAccessTokenUseCase.execute(businessKey, requireAccessToken(accessToken)));
    }

    @POST
    @Path("/accept")
    public OfferResponse accept(
            @PathParam("businessKey") String businessKey,
            @HeaderParam("X-Offer-Access-Token") String accessToken) {
        return OfferResponse.from(acceptOfferUseCase.execute(businessKey, requireAccessToken(accessToken)));
    }

    @POST
    @Path("/reject")
    public OfferResponse reject(
            @PathParam("businessKey") String businessKey,
            @HeaderParam("X-Offer-Access-Token") String accessToken) {
        return OfferResponse.from(rejectOfferUseCase.execute(businessKey, requireAccessToken(accessToken)));
    }

    @POST
    @Path("/changes")
    public OfferResponse requestChanges(
            @PathParam("businessKey") String businessKey,
            @HeaderParam("X-Offer-Access-Token") String accessToken,
            ChangeRequest request) {
        if (request == null || request.description() == null || request.description().isBlank()) {
            throw new BadRequestException("A change request description is required");
        }
        return OfferResponse.from(requestOfferChangesUseCase.execute(
                businessKey, requireAccessToken(accessToken), request.description()));
    }

    private String requireAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new NotAuthorizedException("An offer access token is required");
        }
        return accessToken;
    }
}
