package de.werkmundo.invoice.infrastructure.offer;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InvoiceOfferLoaderTest {

    private static final String BUSINESS_KEY = "offer-test";
    private static final String OWNER = "owner-test";
    private static final String AUTHORIZATION = "Bearer test-token";

    @Test
    void loadsOwnAcceptedOfferAndForwardsAuthorization() {
        OfferResponse expected = offer(OWNER, "ANGENOMMEN");

        OfferServiceClient client = (businessKey, authorization) -> {
            assertEquals(BUSINESS_KEY, businessKey);
            assertEquals(AUTHORIZATION, authorization);
            return expected;
        };

        var loader = new InvoiceOfferLoader(client);

        assertSame(expected, loader.load(
                BUSINESS_KEY, OWNER, AUTHORIZATION));
    }

    @Test
    void rejectsForeignOffer() {
        var loader = new InvoiceOfferLoader(
                (businessKey, authorization) ->
                        offer("other-owner", "ANGENOMMEN"));

        assertThrows(ForbiddenException.class,
                () -> loader.load(BUSINESS_KEY, OWNER, AUTHORIZATION));
    }

    @Test
    void rejectsOfferThatHasNotBeenAccepted() {
        var loader = new InvoiceOfferLoader(
                (businessKey, authorization) ->
                        offer(OWNER, "VERSENDET"));

        WebApplicationException exception = assertThrows(
                WebApplicationException.class,
                () -> loader.load(BUSINESS_KEY, OWNER, AUTHORIZATION));

        assertEquals(409, exception.getResponse().getStatus());
    }

    @Test
    void rejectsMissingAuthorizationBeforeCallingOfferService() {
        var loader = new InvoiceOfferLoader(
                (businessKey, authorization) -> {
                    fail("Ohne Token darf kein Aufruf erfolgen");
                    return null;
                });

        assertThrows(NotAuthorizedException.class,
                () -> loader.load(BUSINESS_KEY, OWNER, null));
    }

    @Test
    void rejectsMismatchedOfferResponse() {
        var loader = new InvoiceOfferLoader(
                (businessKey, authorization) -> new OfferResponse(
                        "different-offer",
                        "ANGENOMMEN",
                        "customer-test",
                        OWNER,
                        List.of(position())
                ));

        WebApplicationException exception = assertThrows(
                WebApplicationException.class,
                () -> loader.load(BUSINESS_KEY, OWNER, AUTHORIZATION));

        assertEquals(502, exception.getResponse().getStatus());
    }

    @Test
    void rejectsOfferWithoutCustomer() {
        var loader = new InvoiceOfferLoader(
                (businessKey, authorization) -> new OfferResponse(
                        BUSINESS_KEY,
                        "ANGENOMMEN",
                        null,
                        OWNER,
                        List.of(position())
                ));

        WebApplicationException exception = assertThrows(
                WebApplicationException.class,
                () -> loader.load(BUSINESS_KEY, OWNER, AUTHORIZATION));

        assertEquals(422, exception.getResponse().getStatus());
    }

    @Test
    void rejectsOfferWithoutPositions() {
        var loader = new InvoiceOfferLoader(
                (businessKey, authorization) -> new OfferResponse(
                        BUSINESS_KEY,
                        "ANGENOMMEN",
                        "customer-test",
                        OWNER,
                        List.of()
                ));

        WebApplicationException exception = assertThrows(
                WebApplicationException.class,
                () -> loader.load(BUSINESS_KEY, OWNER, AUTHORIZATION));

        assertEquals(422, exception.getResponse().getStatus());
    }

    private OfferResponse offer(String owner, String status) {
        return new OfferResponse(
                BUSINESS_KEY,
                status,
                "customer-test",
                owner,
                List.of(position())
        );
    }

    private OfferPositionResponse position() {
        return new OfferPositionResponse(
                "Testhersteller",
                "Material",
                new BigDecimal("2"),
                "Stk.",
                "catalog-test",
                new BigDecimal("12.50"),
                1,
                "MATERIAL"
        );
    }
}