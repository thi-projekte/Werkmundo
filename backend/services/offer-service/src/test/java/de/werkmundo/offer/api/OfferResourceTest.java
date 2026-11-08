package de.werkmundo.offer.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.werkmundo.offer.api.dto.CreateOfferRequest;
import de.werkmundo.offer.api.dto.OfferResponse;
import de.werkmundo.offer.api.dto.UpdateOfferRequest;
import de.werkmundo.offer.application.usecase.offer.ApproveOfferUseCase;
import de.werkmundo.offer.application.usecase.offer.CancelOfferUseCase;
import de.werkmundo.offer.application.usecase.offer.CreateOfferUseCase;
import de.werkmundo.offer.application.usecase.offer.GetOfferUseCase;
import de.werkmundo.offer.application.usecase.offer.ListOffersUseCase;
import de.werkmundo.offer.application.usecase.offer.MarkOfferSentUseCase;
import de.werkmundo.offer.application.usecase.offer.UpdateOfferUseCase;
import de.werkmundo.offer.domain.Offer;
import de.werkmundo.offer.domain.OfferRepository;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OfferResourceTest {
    private InMemoryOfferRepository repository;
    private OfferResource resource;

    @BeforeEach
    void setUp() {
        repository = new InMemoryOfferRepository();
        resource = new OfferResource(
                new CreateOfferUseCase(repository),
                new GetOfferUseCase(repository),
                new ListOffersUseCase(repository),
                new UpdateOfferUseCase(repository),
                new ApproveOfferUseCase(repository),
                new MarkOfferSentUseCase(repository),
                new CancelOfferUseCase(repository));
    }

    @Test
    void createsOfferForAuthenticatedCraftsman() {
        CreateOfferRequest request = new CreateOfferRequest(
                "offer-1", "customer-1", null, List.of(), List.of());

        try (Response response = resource.create(request, securityContext("craftsman-1"))) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            OfferResponse offer = assertInstanceOf(OfferResponse.class, response.getEntity());
            assertEquals("offer-1", offer.businessKey());
            assertEquals(1, resource.list(null, securityContext("craftsman-1")).size());
        }
    }

    @Test
    void rejectsOwnerActionsWithoutAuthenticatedPrincipal() {
        assertThrows(
                NotAuthorizedException.class,
                () -> resource.list(null, securityContext(null)));
    }

    @Test
    void cannotUpdateAnotherCraftsmansOffer() {
        resource.create(
                new CreateOfferRequest("offer-1", "customer-1", null, List.of(), List.of()),
                securityContext("craftsman-1"));

        UpdateOfferRequest request = new UpdateOfferRequest(
                "customer-1", null, List.of(), List.of());

        assertThrows(
                java.util.NoSuchElementException.class,
                () -> resource.update("offer-1", request, securityContext("craftsman-2")));
        assertEquals("craftsman-1", repository.findByBusinessKey("offer-1").orElseThrow().getCraftsmanId());
    }

    private SecurityContext securityContext(String principalName) {
        return new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return principalName == null ? null : () -> principalName;
            }

            @Override
            public boolean isUserInRole(String role) {
                return false;
            }

            @Override
            public boolean isSecure() {
                return true;
            }

            @Override
            public String getAuthenticationScheme() {
                return "test";
            }
        };
    }

    private static class InMemoryOfferRepository implements OfferRepository {
        private final List<Offer> offers = new ArrayList<>();

        @Override
        public Offer save(Offer offer) {
            offers.removeIf(existing -> existing.getBusinessKey().equals(offer.getBusinessKey()));
            offers.add(offer);
            return offer;
        }

        @Override
        public Optional<Offer> findByBusinessKey(String businessKey) {
            return offers.stream()
                    .filter(offer -> offer.getBusinessKey().equals(businessKey))
                    .findFirst();
        }

        @Override
        public List<Offer> findAllByCraftsmanId(String craftsmanId) {
            return offers.stream()
                    .filter(offer -> craftsmanId.equals(offer.getCraftsmanId()))
                    .toList();
        }
    }
}
