package de.werkmundo.gateway;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
@QuarkusTestResource(BackendTestResource.class)
class GatewayRoutingTest {

    @Test
    void healthIsPublicAndStatusRequiresLogin() {
        given().get("/q/health").then().statusCode(200);
        given().get("/gateway/status").then().statusCode(401);
    }

    @Test
    void protectedServiceRoutesRequireLogin() {
        for (String path : List.of(
                "/svc/user/api/users/me",
                "/svc/offer/offers",
                "/svc/invoice/documents",
                "/svc/catalog/catalog/material")) {
            given().get(path).then().statusCode(401);
        }

        given().contentType("application/json").body("{}")
                .put("/svc/user/api/users/profile")
                .then().statusCode(401);

        given().delete("/svc/offer/api/offers/test")
                .then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "gateway-test", roles = "OWNER")
    void routesReachTheCorrectServices() {
        String[][] routes = {
                {"user", "/api/users/me"},
                {"offer", "/offers"},
                {"invoice", "/documents"},
                {"catalog", "/catalog/material"}
        };

        for (String[] route : routes) {
            given().get("/svc/" + route[0] + route[1])
                    .then().statusCode(200)
                    .body("service", equalTo(route[0]))
                    .body("path", equalTo(route[1]))
                    .body("method", equalTo("GET"));
        }
    }

    @Test
    @TestSecurity(user = "gateway-test", roles = "OWNER")
    void forwardsJsonAndQueryParameters() {
        String json = "{\"title\":\"Gateway-Test\",\"menge\":2}";

        given().contentType("application/json").body(json)
                .post("/svc/offer/offers/drafts?source=gateway-test")
                .then().statusCode(200)
                .body("service", equalTo("offer"))
                .body("path", equalTo("/offers/drafts"))
                .body("query", equalTo("source=gateway-test"))
                .body("method", equalTo("POST"))
                .body("body", equalTo(json));
    }

    @Test
    void registrationAndPasswordResetArePublic() {
        for (String path : List.of(
                "/api/users/register",
                "/api/users/password-reset/initiate")) {
            given().contentType("application/json").body("{}")
                    .post("/svc/user" + path)
                    .then().statusCode(200)
                    .body("service", equalTo("user"))
                    .body("path", equalTo(path));
        }
    }

    @Test
    void customerLinksDoNotRequireUserLogin() {
        given().get("/svc/offer/angebote/annahme/test-token")
                .then().statusCode(200)
                .body("path", equalTo("/angebote/annahme/test-token"));

        given().contentType("application/json").body("{}")
                .post("/svc/offer/angebote/annahme/test-token")
                .then().statusCode(200);

        given().header("X-Offer-Access-Token", "test-access-token")
                .get("/svc/offer/api/offers/test/customer")
                .then().statusCode(200)
                .body("offerToken", equalTo("test-access-token"));

        for (String action : List.of("accept", "reject", "changes")) {
            given().header("X-Offer-Access-Token", "test-access-token")
                    .contentType("application/json").body("{}")
                    .post("/svc/offer/api/offers/test/customer/" + action)
                    .then().statusCode(200)
                    .body("offerToken", equalTo("test-access-token"));
        }
    }

    @Test
    void publicExceptionsDoNotExposeOtherOfferRoutes() {
        given().get("/svc/offer/api/offers/test")
                .then().statusCode(401);

        given().contentType("application/json").body("{}")
                .post("/svc/offer/api/offers/test/customer/approve")
                .then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "gateway-test", roles = "OWNER")
    void preservesBackendErrorResponses() {
        given().get("/svc/invoice/documents/missing")
                .then().statusCode(404)
                .body("service", equalTo("invoice"))
                .body("path", equalTo("/documents/missing"));
    }
}