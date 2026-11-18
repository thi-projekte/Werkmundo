package de.werkmundo.invoice.infrastructure;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;

import java.util.function.Supplier;

public final class RemoteServiceErrors {

    private RemoteServiceErrors() {
    }

    public static <T> T call(String serviceName, Supplier<T> operation) {
        try {
            return operation.get();
        } catch (WebApplicationException exception) {
            int status = exception.getResponse().getStatus();

            if (status == 401 || status == 403 || status == 404) {
                throw exception;
            }

            if (status == 429 || status == 503 || status == 504) {
                throw new WebApplicationException(
                        serviceName + " ist vorübergehend nicht verfügbar",
                        503);
            }

            throw new WebApplicationException(
                    serviceName + " hat eine unerwartete Antwort geliefert",
                    502);
        } catch (ProcessingException exception) {
            throw new WebApplicationException(
                    serviceName + " konnte nicht zuverlässig angesprochen werden",
                    503);
        }
    }
}