package de.werkmundo.invoice.infrastructure;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RemoteServiceErrorsTest {

    @Test
    void returnsSuccessfulResult() {
        String result = RemoteServiceErrors.call(
                "Offer-Service", () -> "Ergebnis");

        assertEquals("Ergebnis", result);
    }

    @Test
    void preservesAuthenticationAuthorizationAndNotFoundErrors() {
        for (int status : List.of(401, 403, 404)) {
            var original = new WebApplicationException(status);

            WebApplicationException actual = assertThrows(
                    WebApplicationException.class,
                    () -> RemoteServiceErrors.call("Offer-Service", () -> {
                        throw original;
                    }));

            assertSame(original, actual);
            assertEquals(status, actual.getResponse().getStatus());
        }
    }

    @Test
    void mapsTemporaryHttpErrorsToServiceUnavailable() {
        for (int status : List.of(429, 503, 504)) {
            WebApplicationException actual = assertThrows(
                    WebApplicationException.class,
                    () -> RemoteServiceErrors.call("User-Service", () -> {
                        throw new WebApplicationException(status);
                    }));

            assertEquals(503, actual.getResponse().getStatus());
        }
    }

    @Test
    void mapsUnexpectedHttpErrorsToBadGateway() {
        for (int status : List.of(400, 500)) {
            WebApplicationException actual = assertThrows(
                    WebApplicationException.class,
                    () -> RemoteServiceErrors.call("Offer-Service", () -> {
                        throw new WebApplicationException(status);
                    }));

            assertEquals(502, actual.getResponse().getStatus());
        }
    }

    @Test
    void mapsProcessingFailureWithoutRetrying() {
        var calls = new AtomicInteger();

        WebApplicationException actual = assertThrows(
                WebApplicationException.class,
                () -> RemoteServiceErrors.call("Offer-Service", () -> {
                    calls.incrementAndGet();
                    throw new ProcessingException("Simulierter Fehler");
                }));

        assertEquals(503, actual.getResponse().getStatus());
        assertEquals(1, calls.get());
    }
}