/**
 * REST-Schicht des ai-service (eingehend).
 *
 * <ul>
 *   <li>{@code POST /ai/process} – Aufruf durch die Process Engine (HTTP-Connector). Antwortet
 *       sofort mit 202; das Ergebnis kommt spaeter als Message {@code ergebnisKI}.</li>
 *   <li>{@code POST /ai/analyze} – gleicher Payload, aber synchron mit dem Ergebnis in der
 *       Antwort. Fuer lokale Tests ohne Process Engine.</li>
 * </ul>
 *
 * <p>Aufgaben: HTTP-Request annehmen, DTO in den Domain-{@code AnalyseAuftrag} uebersetzen,
 * an {@link de.winfprojekt.werkmundo.aiservice.application.AnalyzeVoiceInputUseCase}
 * delegieren, HTTP-Antwort bauen. <b>Keine</b> Business-Logik und keine LLM-Aufrufe hier.
 * HTTP-DTOs liegen in {@code api/dto}.
 */
package de.winfprojekt.werkmundo.aiservice.api;
