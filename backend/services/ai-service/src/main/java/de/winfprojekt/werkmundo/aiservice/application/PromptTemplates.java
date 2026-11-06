package de.winfprojekt.werkmundo.aiservice.application;

import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Laedt die System-Prompts aus {@code src/main/resources/prompts/}. So koennen Prompts ohne
 * Java-Aenderung angepasst werden. Fehlt eine Datei, schlaegt der Start fehl (fail-fast).
 *
 * <p>Die Prompts von Call 1 sind wortgleich zur Modellwahl-Evaluation (siehe
 * {@code docs/referenz/ai-service-modellwahl.md}) - Aenderungen bitte neu evaluieren.
 */
@ApplicationScoped
public class PromptTemplates {

    private final String call1Erstangebot = load("prompts/call1-erstangebot.txt");
    private final String call1Korrektur = load("prompts/call1-korrektur.txt");
    private final String call2Produktauswahl = load("prompts/call2-produktauswahl.txt");

    public String call1Erstangebot() {
        return call1Erstangebot;
    }

    public String call1Korrektur() {
        return call1Korrektur;
    }

    public String call2Produktauswahl() {
        return call2Produktauswahl;
    }

    static String load(String resource) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = PromptTemplates.class.getClassLoader();
        }
        try (InputStream is = cl.getResourceAsStream(resource)) {
            if (is == null) {
                throw new IllegalStateException("Prompt-Datei fehlt im Classpath: " + resource);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8).strip();
        } catch (IOException e) {
            throw new UncheckedIOException("Prompt-Datei nicht lesbar: " + resource, e);
        }
    }
}
