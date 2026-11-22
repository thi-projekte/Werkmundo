/**
 * Fachliche Kernstrukturen des ai-service.
 *
 * <p>Plain Java-Records ohne Framework- oder HTTP-Bezug:
 * <ul>
 *   <li>{@link de.werkmundo.ai.domain.AnalyseAuftrag} – was analysiert werden soll
 *       (inkl. {@link de.werkmundo.ai.domain.ProcessType})</li>
 *   <li>{@link de.werkmundo.ai.domain.Vorlage},
 *       {@link de.werkmundo.ai.domain.Angebotspositionen},
 *       {@link de.werkmundo.ai.domain.Position} – Angebotsdaten</li>
 *   <li>{@link de.werkmundo.ai.domain.ErgebnisKi} – Ergebnis an die Process Engine</li>
 *   <li>{@link de.werkmundo.ai.domain.ProduktKandidat} – Katalogprodukt fuer LLM-Call 2</li>
 * </ul>
 *
 * <p><b>Datenschutz-Constraint:</b> Keine dieser Strukturen enthaelt einen Preis. Preise werden
 * der KI nie uebergeben; der offer-service ergaenzt sie nach der KI-Verarbeitung.
 */
package de.werkmundo.ai.domain;
