package de.winfprojekt.werkmundo.aiservice.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.winfprojekt.werkmundo.aiservice.domain.Angebotspositionen;
import de.winfprojekt.werkmundo.aiservice.domain.ErgebnisKi;
import de.winfprojekt.werkmundo.aiservice.domain.Position;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;

/**
 * Parst die rohe Modell-Ausgabe von LLM-Call 1 robust zu einem {@link ErgebnisKi}.
 *
 * <p>Toleriert Markdown-Code-Fences, einen fehlenden Wrapper
 * ({@code {leistungen, material, notizen}} direkt auf oberster Ebene) und fehlende
 * Teil-Arrays. Ein eventuelles {@code preis}-Feld wird ignoriert. Ist die Ausgabe kein
 * JSON-Objekt, wird eine {@link LlmException} geworfen.
 */
@ApplicationScoped
public class ErgebnisKiParser {

    private final ObjectMapper objectMapper;

    public ErgebnisKiParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ErgebnisKi parse(String rawOutput) {
        String text = rawOutput == null ? "" : rawOutput.strip();

        if (text.startsWith("```")) {
            text = text.replaceFirst("(?s)^```(?:json)?\\s*", "")
                    .replaceFirst("(?s)\\s*```\\s*$", "")
                    .strip();
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(text);
        } catch (JsonProcessingException e) {
            throw new LlmException("LLM-Antwort ist kein gueltiges JSON.", e);
        }
        if (root == null || !root.isObject()) {
            throw new LlmException("LLM-Antwort ist kein JSON-Objekt.");
        }

        JsonNode sap = root.get("strukturierteAngebotspositionen");
        if (sap == null && (root.has("leistungen") || root.has("material") || root.has("notizen"))) {
            sap = root;
        }

        return new ErgebnisKi(
                new Angebotspositionen(
                        readPositions(sap, "leistungen"),
                        readPositions(sap, "material"),
                        readStrings(sap, "notizen")),
                readStrings(root, "korrekturvorschlaege"),
                readNumber(root, "geschaetzteArbeitsdauerStunden"));
    }

    private static List<Position> readPositions(JsonNode parent, String field) {
        List<Position> out = new ArrayList<>();
        JsonNode arr = parent == null ? null : parent.get(field);
        if (arr == null || !arr.isArray()) {
            return out;
        }
        for (JsonNode n : arr) {
            if (n != null && n.isObject()) {
                out.add(new Position(
                        readText(n, "bezeichnung"),
                        readText(n, "beschreibung"),
                        readNumber(n, "menge"),
                        readText(n, "einheit")));
            }
        }
        return out;
    }

    private static List<String> readStrings(JsonNode parent, String field) {
        List<String> out = new ArrayList<>();
        JsonNode arr = parent == null ? null : parent.get(field);
        if (arr == null || !arr.isArray()) {
            return out;
        }
        for (JsonNode n : arr) {
            if (n != null && !n.isNull()) {
                out.add(n.asText());
            }
        }
        return out;
    }

    private static String readText(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static Double readNumber(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() || !v.isNumber() ? null : v.asDouble();
    }
}
