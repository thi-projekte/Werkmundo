package de.werkmundo.offer.offer;

import de.werkmundo.offer.api.H2StartupTestProfile;
import de.werkmundo.offer.user.CustomerDTO;
import de.werkmundo.offer.user.UserServiceClient;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import jakarta.ws.rs.NotFoundException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
@TestProfile(H2StartupTestProfile.class)
@TestSecurity(user = "assignment-owner", roles = "OWNER")
@OidcSecurity(claims = {
        @Claim(key = "sub", value = "assignment-owner")
})
class AssignOfferCustomerTest {

    private static final String OWNER = "assignment-owner";
    private final String businessKey = "assignment-" + UUID.randomUUID();

    @InjectMock
    @RestClient
    UserServiceClient userServiceClient;

    @BeforeEach
    void resetClient() {
        reset(userServiceClient);
    }

    @AfterEach
    void removeTestOffer() {
        QuarkusTransaction.requiringNew().run(() ->
                Offer.delete("businessKey", businessKey));
    }

    @Test
    void assignsOwnCustomerAndPersistsAssignment() {
        createOffer(OWNER, Offer.STATUS_ENTWURF);

        CustomerDTO customer = new CustomerDTO();
        customer.id = 42L;
        when(userServiceClient.getCustomer(42L)).thenReturn(customer);

        given()
                .contentType("application/json")
                .body("""
                        {"customerId": 42}
                        """)
                .when()
                .put("/offers/" + businessKey + "/customer")
                .then()
                .statusCode(200)
                .body("customerId", equalTo("42"))
                .body("status", equalTo(Offer.STATUS_ENTWURF));

        assertEquals("42", storedCustomerId());
        verify(userServiceClient).getCustomer(42L);
    }

    @Test
    void rejectsForeignOfferBeforeCallingUserService() {
        createOffer("other-owner", Offer.STATUS_ENTWURF);

        given()
                .contentType("application/json")
                .body("""
                        {"customerId": 42}
                        """)
                .when()
                .put("/offers/" + businessKey + "/customer")
                .then()
                .statusCode(403);

        assertNull(storedCustomerId());
        verifyNoInteractions(userServiceClient);
    }

    @Test
    void rejectsAssignmentAfterDraftStage() {
        createOffer(OWNER, Offer.STATUS_VERSENDET);

        given()
                .contentType("application/json")
                .body("""
                        {"customerId": 42}
                        """)
                .when()
                .put("/offers/" + businessKey + "/customer")
                .then()
                .statusCode(409);

        assertNull(storedCustomerId());
        verifyNoInteractions(userServiceClient);
    }

    @Test
    void doesNotAssignCustomerRejectedByUserService() {
        createOffer(OWNER, Offer.STATUS_ENTWURF);
        when(userServiceClient.getCustomer(42L))
                .thenThrow(new NotFoundException());

        given()
                .contentType("application/json")
                .body("""
                        {"customerId": 42}
                        """)
                .when()
                .put("/offers/" + businessKey + "/customer")
                .then()
                .statusCode(404);

        assertNull(storedCustomerId());
        verify(userServiceClient).getCustomer(42L);
    }

    private void createOffer(String owner, String status) {
        QuarkusTransaction.requiringNew().run(() -> {
            Offer offer = new Offer();
            offer.businessKey = businessKey;
            offer.handwerkerId = owner;
            offer.status = status;
            offer.persist();
        });
    }

    private String storedCustomerId() {
        return QuarkusTransaction.requiringNew().call(() -> {
            Offer offer = Offer.find("businessKey", businessKey).firstResult();
            assertNotNull(offer);
            return offer.customerId;
        });
    }
}