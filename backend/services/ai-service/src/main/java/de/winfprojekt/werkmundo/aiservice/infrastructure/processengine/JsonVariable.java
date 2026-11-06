package de.winfprojekt.werkmundo.aiservice.infrastructure.processengine;

/**
 * Typisierte Prozessvariable im Format der Camunda-7-/CIB-seven-REST-API.
 *
 * <p>Typ {@code "String"}: Die Process Engine liest {@code ergebnisKI} per {@code .toString()}
 * und parst es selbst mit {@code S(...)} (Spin) - so ist es im BPMN umgesetzt.
 */
public record JsonVariable(String value, String type) {

    public static JsonVariable ofJsonString(String json) {
        return new JsonVariable(json, "String");
    }
}
