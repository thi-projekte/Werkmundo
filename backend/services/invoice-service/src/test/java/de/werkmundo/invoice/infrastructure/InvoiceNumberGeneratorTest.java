package de.werkmundo.invoice.infrastructure;

import de.werkmundo.invoice.document.infrastructure.PostgresDocumentTestProfile;
import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.sql.Connection;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(PostgresDocumentTestProfile.class)
@EnabledIfEnvironmentVariable(named = "DOCUMENT_DB_TESTS", matches = "true")
class InvoiceNumberGeneratorTest {

    @Inject
    AgroalDataSource dataSource;

    @Inject
    InvoiceNumberGenerator generator;

    @Test
    void generatesDistinctNumbersConcurrently() throws Exception {
        var ready = new java.util.concurrent.CountDownLatch(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Callable<String> task = () -> {
                ready.countDown();
                if (!start.await(10, java.util.concurrent.TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Concurrent test start timed out");
                }
                try (Connection connection = dataSource.getConnection()) {
                    return generator.nextNumber(connection);
                }
            };
            var first = pool.submit(task);
            var second = pool.submit(task);
            assertTrue(ready.await(10, java.util.concurrent.TimeUnit.SECONDS));
            start.countDown();
            String a = first.get(20, java.util.concurrent.TimeUnit.SECONDS);
            String b = second.get(20, java.util.concurrent.TimeUnit.SECONDS);
            assertTrue(a.matches("^RE-[0-9]{6}-[0-9]{4}$"));
            assertTrue(b.matches("^RE-[0-9]{6}-[0-9]{4}$"));
            assertNotEquals(a, b);
        } finally {
            start.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void generatesDistinctSequentialInvoiceNumbers() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String first = generator.nextNumber(connection);
                String second = generator.nextNumber(connection);

                assertTrue(first.matches("^RE-[0-9]{6}-[0-9]{4}$"));
                assertTrue(second.matches("^RE-[0-9]{6}-[0-9]{4}$"));
                assertNotEquals(first, second);
                assertEquals(first.substring(0, 10), second.substring(0, 10));
                assertEquals(Integer.parseInt(first.substring(10)) + 1,
                        Integer.parseInt(second.substring(10)));
            } finally {
                connection.rollback();
            }
        }
    }
}
