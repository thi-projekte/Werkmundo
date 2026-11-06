package de.winfprojekt.werkmundo.aiservice.infrastructure.processengine;

import java.util.Map;

/**
 * Body fuer {@code POST /engine-rest/message}. Die Engine findet die wartende Prozessinstanz
 * ueber {@code businessKey} + {@code messageName} und schreibt die {@code processVariables}.
 *
 * <pre>
 * { "messageName": "ergebnisKI", "businessKey": "BK-123",
 *   "processVariables": { "ergebnisKI": { "value": "{...}", "type": "String" } } }
 * </pre>
 */
public record CamundaCorrelationRequest(
        String messageName,
        String businessKey,
        Map<String, JsonVariable> processVariables
) {

    public static CamundaCorrelationRequest ergebnisKI(String businessKey, String ergebnisKiJson) {
        return new CamundaCorrelationRequest(
                WorkflowMessageNames.ERGEBNIS_KI_MESSAGE,
                businessKey,
                Map.of(WorkflowMessageNames.ERGEBNIS_KI_VARIABLE, JsonVariable.ofJsonString(ergebnisKiJson)));
    }
}
