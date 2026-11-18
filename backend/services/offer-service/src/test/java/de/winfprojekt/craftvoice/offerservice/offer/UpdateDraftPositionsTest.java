package de.winfprojekt.craftvoice.offerservice.offer;

import de.werkmundo.offer.api.H2StartupTestProfile;
import de.winfprojekt.craftvoice.offerservice.common.OfferPositionType;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(H2StartupTestProfile.class)
@TestSecurity(user = "positions-owner", roles = "OWNER")
@OidcSecurity(claims = {
        @Claim(key = "sub", value = "positions-owner")
})
class UpdateDraftPositionsTest {

    private static final String OWNER = "positions-owner";
    private final String businessKey = "positions-" + UUID.randomUUID();

    @AfterEach
    void removeTestOffer() {
        QuarkusTransaction.requiringNew().run(() -> {
            Offer offer = Offer.find("businessKey", businessKey).firstResult();
            if (offer != null) {
                offer.delete();
            }
        });
    }

    @Test
    void persistsPositionsAndRoundsEachLineBeforeAdding() {
        createOffer(OWNER, Offer.STATUS_ENTWURF, OfferPositionType.OTHER);

        update("""
                {
                  "positions": [
                    {
                      "bezeichnung": "Erste Position",
                      "menge": 0.5,
                      "einheit": "Stk.",
                      "einzelPreis": 0.01
                    },
                    {
                      "bezeichnung": "Zweite Position",
                      "menge": 0.5,
                      "einheit": "Stk.",
                      "einzelPreis": 0.01
                    }
                  ]
                }
                """, 200);

        QuarkusTransaction.requiringNew().run(() -> {
            Offer offer = loadOffer();

            assertEquals("42", offer.customerId);
            assertEquals(Offer.STATUS_ENTWURF, offer.status);
            assertEquals(2, offer.positions.size());
            assertEquals(0,
                    new BigDecimal("0.02").compareTo(offer.gesamtPreis));

            for (OfferPosition position : offer.positions) {
                assertEquals(OfferPositionType.OTHER, position.type);
                assertEquals(0,
                        new BigDecimal("0.01")
                                .compareTo(position.positionsPreis));
            }

            assertTrue(offer.positions.stream()
                    .anyMatch(position -> Integer.valueOf(1)
                            .equals(position.reihenfolge)));
            assertTrue(offer.positions.stream()
                    .anyMatch(position -> Integer.valueOf(2)
                            .equals(position.reihenfolge)));
        });
    }

    @Test
    void allowsRemovingAllManualPositions() {
        createOffer(OWNER, Offer.STATUS_ENTWURF, OfferPositionType.OTHER);

        update("""
                {"positions": []}
                """, 200);

        QuarkusTransaction.requiringNew().run(() -> {
            Offer offer = loadOffer();

            assertTrue(offer.positions.isEmpty());
            assertEquals(0, BigDecimal.ZERO.compareTo(offer.gesamtPreis));
            assertEquals("42", offer.customerId);
        });
    }

    @Test
    void rejectsForeignOfferWithoutChangingPositions() {
        createOffer("other-owner",
                Offer.STATUS_ENTWURF, OfferPositionType.OTHER);

        update("""
                {"positions": []}
                """, 403);

        assertOriginalPositionRemains();
    }

    @Test
    void rejectsChangesOutsideDraftStatus() {
        createOffer(OWNER,
                Offer.STATUS_VERSENDET, OfferPositionType.OTHER);

        update("""
                {"positions": []}
                """, 409);

        assertOriginalPositionRemains();
    }

    @Test
    void rejectsInvalidQuantityWithoutChangingPositions() {
        createOffer(OWNER, Offer.STATUS_ENTWURF, OfferPositionType.OTHER);

        update("""
                {
                  "positions": [
                    {
                      "bezeichnung": "Ungueltig",
                      "menge": -1,
                      "einheit": "Stk.",
                      "einzelPreis": 12.50
                    }
                  ]
                }
                """, 400);

        assertOriginalPositionRemains();
    }

    @Test
    void doesNotOverwriteCatalogPositions() {
        createOffer(OWNER, Offer.STATUS_ENTWURF,
                OfferPositionType.MATERIAL);

        update("""
                {"positions": []}
                """, 409);

        assertOriginalPositionRemains();
    }

    private void update(String body, int expectedStatus) {
        given()
                .contentType("application/json")
                .body(body)
                .when()
                .put("/offers/" + businessKey + "/draft-positions")
                .then()
                .statusCode(expectedStatus);
    }

    private void createOffer(
            String owner, String status, OfferPositionType type) {

        QuarkusTransaction.requiringNew().run(() -> {
            Offer offer = new Offer();
            offer.businessKey = businessKey;
            offer.handwerkerId = owner;
            offer.customerId = "42";
            offer.status = status;
            offer.gesamtPreis = new BigDecimal("10.00");

            OfferPosition position = new OfferPosition();
            position.offer = offer;
            position.bezeichnung = "Originalposition";
            position.menge = BigDecimal.ONE;
            position.einheit = "Stk.";
            position.einzelPreis = new BigDecimal("10.00");
            position.positionsPreis = new BigDecimal("10.00");
            position.reihenfolge = 1;
            position.type = type;

            if (type == OfferPositionType.MATERIAL) {
                position.katalogProduktId = UUID.randomUUID().toString();
            }

            offer.positions.add(position);
            offer.persist();
        });
    }

    private void assertOriginalPositionRemains() {
        QuarkusTransaction.requiringNew().run(() -> {
            Offer offer = loadOffer();

            assertEquals(1, offer.positions.size());
            assertEquals("Originalposition",
                    offer.positions.getFirst().bezeichnung);
            assertEquals(0,
                    new BigDecimal("10.00").compareTo(offer.gesamtPreis));
        });
    }

    private Offer loadOffer() {
        Offer offer = Offer.find("businessKey", businessKey).firstResult();
        assertNotNull(offer);
        return offer;
    }
}