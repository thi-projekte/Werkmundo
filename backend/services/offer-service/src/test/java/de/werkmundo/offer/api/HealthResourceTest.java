package de.werkmundo.offer.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class HealthResourceTest {
    @Test
    void reportsThatHttpServiceIsRunning() {
        given()
                .when().get("/health")
                .then()
                .statusCode(200)
                .body(is("UP"));
    }
}
