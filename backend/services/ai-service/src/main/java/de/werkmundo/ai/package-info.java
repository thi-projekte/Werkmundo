/**
 * Root-Package des ai-service (Squad Handwerker).
 *
 * <p>Der ai-service ist ein zustandsloser HTTP-Service. Die Process Engine ruft ihn per
 * HTTP-Connector auf ({@code POST /ai/process}). Er analysiert einen Sprachschnipsel
 * (Erstangebot) bzw. einen Korrekturschnipsel (Korrektur) in zwei LLM-Schritten und sendet
 * das strukturierte Ergebnis als Message {@code ergebnisKI} per {@code businessKey} an die
 * Engine zurueck.
 *
 * <p>Schichten (Zielstruktur aus Issue #29, gleiche Struktur wie die anderen Services):
 * <ul>
 *   <li>{@link de.werkmundo.ai.api} – REST-Endpoints und HTTP-DTOs (eingehend)</li>
 *   <li>{@link de.werkmundo.ai.application} – Use-Case und Pipeline-Schritte (Call 1, Call 2)
 *       sowie die Ports zu externen Systemen</li>
 *   <li>{@link de.werkmundo.ai.domain} – fachliche Strukturen ohne Framework-Logik</li>
 *   <li>{@link de.werkmundo.ai.infrastructure} – Adapter fuer MegaLLM, catalog-service
 *       und Process Engine (implementieren die Ports)</li>
 * </ul>
 *
 * <p>Abhaengigkeitsrichtung: {@code api -> application -> domain} und
 * {@code infrastructure -> application/domain}. Die application-Schicht kennt keine
 * konkrete Technik (kein MegaLLM, kein Camunda, kein REST-Client).
 */
package de.werkmundo.ai;
