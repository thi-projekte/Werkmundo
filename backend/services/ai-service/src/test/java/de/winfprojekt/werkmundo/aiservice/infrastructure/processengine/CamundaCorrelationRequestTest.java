package de.winfprojekt.werkmundo.aiservice.infrastructure.processengine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Format der Message-Korrelation (contracts/workflow/ErgebnisKiMessage.json). */
class CamundaCorrelationRequestTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void topLevelFelder() {
        JsonNode json = mapper.valueToTree(CamundaCorrelationRequest.ergebnisKI("BK-001", "{}"));
        assertEquals("ergebnisKI", json.get("messageName").asText());
        assertEquals("BK-001", json.get("businessKey").asText());
        assertEquals(3, json.size());
    }

    @Test
    void variable_istStringTyped_undValueIstText() {
        JsonNode variable = mapper.valueToTree(CamundaCorrelationRequest.ergebnisKI("BK-002", "{\"nested\":{\"k\":1}}"))
                .get("processVariables").get("ergebnisKI");
        assertEquals("String", variable.get("type").asText(), "Die PE parst den Inhalt selbst per S()");
        assertTrue(variable.get("value").isTextual());
        assertEquals("{\"nested\":{\"k\":1}}", variable.get("value").asText());
    }
}
