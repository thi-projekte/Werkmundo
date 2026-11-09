/**
 * Fachliche Kernstrukturen des ai-service.
 *
 * <p>Plain Java-Records ohne Framework- oder HTTP-Bezug:
 * <ul>
 *   <li>{@link de.winfprojekt.werkmundo.aiservice.domain.AnalyseAuftrag} – was analysiert werden soll
 *       (inkl. {@link de.winfprojekt.werkmundo.aiservice.domain.ProcessType})</li>
 *   <li>{@link de.winfprojekt.werkmundo.aiservice.domain.Vorlage},
 *       {@link de.winfprojekt.werkmundo.aiservice.domain.Angebotspositionen},
 *       {@link de.winfprojekt.werkmundo.aiservice.domain.Position} – Angebotsdaten</li>
 *   <li>{@link de.winfprojekt.werkmundo.aiservice.domain.ErgebnisKi} – Ergebnis an die Process Engine</li>
 *   <li>{@link de.winfprojekt.werkmundo.aiservice.domain.ProduktKandidat} – Katalogprodukt fuer LLM-Call 2</li>
 * </ul>
 *
 * <p><b>Datenschutz-Constraint:</b> Keine dieser Strukturen enthaelt einen Preis. Preise werden
 * der KI nie uebergeben; der offer-service ergaenzt sie nach der KI-Verarbeitung.
 */
package de.winfprojekt.werkmundo.aiservice.domain;
