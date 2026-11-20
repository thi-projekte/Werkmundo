/**
 * Ports (Interfaces) der application-Schicht zu externen Systemen. So bleibt die Pipeline
 * unabhaengig vom LLM-Anbieter, vom catalog-service und von der Process Engine.
 *
 * <ul>
 *   <li>{@link de.werkmundo.ai.application.port.LlmPort} – Chat-Completion
 *       (Implementierung: {@code infrastructure.llm.MegaLlmAdapter})</li>
 *   <li>{@link de.werkmundo.ai.application.port.CatalogSearchPort} – Produktsuche
 *       (Implementierungen: {@code infrastructure.catalog.MockCatalogSearch} / {@code RestCatalogSearch})</li>
 *   <li>{@link de.werkmundo.ai.application.port.WorkflowNotifier} – Ergebnis an die
 *       Process Engine (Implementierung: {@code infrastructure.processengine.CamundaWorkflowNotifier})</li>
 * </ul>
 */
package de.werkmundo.ai.application.port;
