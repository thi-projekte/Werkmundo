package de.werkmundo.invoice.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;

@QuarkusTest
class InvoiceResourceTest {

    @Test
    void rejectsUnauthenticatedRequest() {
        given()
                .contentType("application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("""
                        {"businessKey": "offer-test"}
                        """)
                .when()
                .post("/invoices/drafts")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "employee-test", roles = "EMPLOYEE")
    void rejectsUserWithoutOwnerRole() {
        given()
                .contentType("application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("""
                        {"businessKey": "offer-test"}
                        """)
                .when()
                .post("/invoices/drafts")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "owner-test", roles = "OWNER")
    void rejectsMissingBusinessKey() {
        given()
                .contentType("application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("{}")
                .when()
                .post("/invoices/drafts")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "owner-test", roles = "OWNER")
    void rejectsMissingIdempotencyKey() {
        given()
                .contentType("application/json")
                .body("""
                        {"businessKey": "offer-test"}
                        """)
                .when()
                .post("/invoices/drafts")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "owner-test", roles = "OWNER")
    void rejectsInvalidIdempotencyKey() {
        given()
                .contentType("application/json")
                .header("Idempotency-Key", "kein-gueltiger-schluessel")
                .body("""
                        {"businessKey": "offer-test"}
                        """)
                .when()
                .post("/invoices/drafts")
                .then()
                .statusCode(400);
    }
}