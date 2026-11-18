package de.werkmundo.catalog;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.File;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@EnabledIfEnvironmentVariable(named = "CATALOG_TEST_DATABASE", matches = "true")
class CatalogImportTest {
    private static final String API = "/catalog/material";
    private static final String OWNER = "catalog-import-test";
    private static final File CATALOG = new File("../../../documents/Hager_Artikelstamm_2026-11.csv");

    @Inject
    AgroalDataSource dataSource;

    @Test
    @TestSecurity(user = OWNER)
    void importsFullCatalogAndKeepsProductIdOnReimport() throws SQLException {
        upload();
        UUID id = readImportedProduct();
        upload();
        assertEquals(id, readImportedProduct());
    }

    @Test
    @TestSecurity(user = OWNER)
    void readEndpointsAreAbsent() {
        given().queryParam("q", "B400603").get(API + "/search").then().statusCode(404);
        given().get(API + "/" + UUID.randomUUID()).then().statusCode(404);
    }

    @Test
    void anonymousUploadIsRejected() {
        given().multiPart("file", CATALOG, "text/csv").post(API + "/import/csv")
                .then().statusCode(401);
    }

    private void upload() {
        given().multiPart("file", CATALOG, "text/csv").post(API + "/import/csv")
                .then().statusCode(200).body(equalTo("15103"));
    }

    private UUID readImportedProduct() throws SQLException {
        String sql = """
                SELECT id, price, (SELECT count(*) FROM material WHERE owner_id = ?) AS total
                FROM material
                WHERE owner_id = ? AND article_number = 'B400603'
                """;
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, OWNER);
            statement.setString(2, OWNER);
            try (var result = statement.executeQuery()) {
                assertTrue(result.next());
                assertEquals(15103, result.getInt("total"));
                assertEquals(new BigDecimal("1.723"), result.getBigDecimal("price"));
                return result.getObject("id", UUID.class);
            }
        }
    }
}
