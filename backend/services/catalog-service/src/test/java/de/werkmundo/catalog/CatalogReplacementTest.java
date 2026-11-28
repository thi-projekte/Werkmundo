package de.werkmundo.catalog;

import de.werkmundo.catalog.domain.Material;
import de.werkmundo.catalog.domain.MaterialRepository;
import de.werkmundo.catalog.domain.CatalogReader;
import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@EnabledIfEnvironmentVariable(named = "CATALOG_TEST_DATABASE", matches = "true")
class CatalogReplacementTest {
    private static final String API = "/catalog/material";
    @Inject MaterialRepository repository;
    @Inject CatalogReader reader;
    @Inject AgroalDataSource dataSource;

    @BeforeEach
    void clear() throws SQLException {
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM material WHERE owner_id IN ('replacement', 'other', 'parallel', 'rollback')");
        }
    }

    @Test
    @TestSecurity(user = "replacement")
    void replacesOwnCatalogAndReturnsSearchWithoutPrices() {
        repository.replace("other", List.of(material("FREMD", "99")));
        upload("ALT;Alter Artikel;Stück;1\nBLEIBT;Bisher;Meter;2\n");
        UUID retained = reader.search("replacement", "BLEIBT", 1).getFirst().id();
        UUID removed = reader.search("replacement", "ALT", 1).getFirst().id();
        upload("BLEIBT;Aktualisiert;Meter;1.723\nNEU;Neuer Artikel;Stück;4\n");
        given().header("X-Handwerker-Id", "other").queryParam("q", "Aktualisiert")
                .get(API + "/search").then().statusCode(200)
                .body("candidates.size()", equalTo(1)).body("candidates[0].id", equalTo(retained.toString()))
                .body("candidates[0]", not(hasKey("price")));
        given().get(API + "/" + retained).then().statusCode(200)
                .body("price", equalTo(1.723f)).body("name", equalTo("Aktualisiert"));
        given().get(API + "/" + removed).then().statusCode(404);
        assertEquals(1, reader.count("other"));
        assertEquals(0, reader.search("replacement", "FREMD", 15).size());
        UUID foreign = reader.search("other", "FREMD", 1).getFirst().id();
        given().header("X-Handwerker-Id", "other").get(API + "/" + foreign).then().statusCode(404);
        given().queryParam("q", "Artikel").queryParam("limit", 0).get(API + "/search").then().statusCode(400);
    }

    @Test
    @TestSecurity(user = "technical", roles = "process-engine")
    void onlyTechnicalRoleCanSelectOwnerAndMustProvideIt() {
        repository.replace("other", List.of(material("FREMD", "99")));
        given().header("X-Handwerker-Id", "other").queryParam("q", "FREMD")
                .get(API + "/search").then().statusCode(200).body("candidates.size()", equalTo(1));
        given().queryParam("q", "FREMD").get(API + "/search").then().statusCode(400);
        UUID foreign = reader.search("other", "FREMD", 1).getFirst().id();
        given().header("X-Handwerker-Id", "other").get(API + "/" + foreign).then().statusCode(200);
    }

    @Test
    void anonymousSearchIsRejected() {
        given().queryParam("q", "Artikel").get(API + "/search").then().statusCode(401);
    }

    @Test
    void databaseFailureRollsBackEarlierUpdates() throws SQLException {
        repository.replace("rollback", List.of(material("ALT", "1"), material("BLEIBT", "2")));
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("ALTER TABLE material ADD CONSTRAINT test_reject_article CHECK (article_number <> 'FAIL')");
            try {
                assertThrows(RuntimeException.class, () -> repository.replace("rollback", List.of(
                        material("BLEIBT", "999"), material("FAIL", "3"))));
                assertEquals(2, reader.count("rollback"));
                assertEquals(0, new BigDecimal("2").compareTo(reader.search("rollback", "BLEIBT", 1).getFirst().price()));
            } finally {
                statement.execute("ALTER TABLE material DROP CONSTRAINT test_reject_article");
            }
        }
    }

    @Test
    void concurrentImportsRemainInvisibleUntilCommitAndNeverMix() throws Exception {
        repository.replace("parallel", List.of(material("ALT", "1")));
        CompletableFuture<Void> first;
        CompletableFuture<Void> second;
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (var statement = connection.createStatement()) {
                statement.execute("SELECT pg_advisory_xact_lock(hashtextextended('parallel', 0))");
                first = CompletableFuture.runAsync(() -> repository.replace("parallel", List.of(material("A1", "1"), material("A2", "2"))));
                second = CompletableFuture.runAsync(() -> repository.replace("parallel", List.of(material("B1", "3"), material("B2", "4"))));
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
                boolean waiting = false;
                while (System.nanoTime() < deadline) {
                    try (var rows = statement.executeQuery("SELECT count(*) FROM pg_locks WHERE locktype = 'advisory' AND NOT granted")) {
                        rows.next();
                        if (rows.getInt(1) >= 2) { waiting = true; break; }
                    }
                    Thread.sleep(20);
                }
                assertTrue(waiting, "Beide Imports müssen auf die Katalogsperre warten");
                assertEquals(List.of("ALT"), reader.search("parallel", "", 10).stream().map(item -> item.articleNumber()).toList());
            } finally {
                connection.rollback();
            }
        }
        first.get(10, TimeUnit.SECONDS);
        second.get(10, TimeUnit.SECONDS);
        var articles = reader.search("parallel", "", 10).stream().map(item -> item.articleNumber()).toList();
        assertTrue(articles.equals(List.of("A1", "A2")) || articles.equals(List.of("B1", "B2")), articles.toString());
    }

    @Test
    void uncommittedReplacementStaysInvisibleAndAbortRestoresOldCatalog() throws Exception {
        repository.replace("rollback", List.of(material("ALT", "1")));
        var staged = new CountDownLatch(1);
        var abort = new CountDownLatch(1);
        var importing = CompletableFuture.runAsync(() -> QuarkusTransaction.requiringNew().run(() -> {
            repository.replace("rollback", List.of(material("NEU", "2")));
            staged.countDown();
            try {
                if (!abort.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Test timeout");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
            throw new IllegalStateException("Import abgebrochen");
        }));
        try {
            assertTrue(staged.await(10, TimeUnit.SECONDS));
            assertEquals(List.of("ALT"), reader.search("rollback", "", 10).stream().map(item -> item.articleNumber()).toList());
        } finally {
            abort.countDown();
        }
        assertThrows(ExecutionException.class, () -> importing.get(10, TimeUnit.SECONDS));
        assertEquals(List.of("ALT"), reader.search("rollback", "", 10).stream().map(item -> item.articleNumber()).toList());
    }

    private Material material(String article, String price) {
        return new Material(article, article, "", "", "", "Stück", new BigDecimal(price), "EUR");
    }

    private void upload(String rows) {
        given().multiPart("file", "catalog.csv", ("articleNumber;name;unit;price\n" + rows).getBytes(StandardCharsets.UTF_8), "text/csv")
                .post(API + "/import/csv").then().statusCode(200);
    }
}
