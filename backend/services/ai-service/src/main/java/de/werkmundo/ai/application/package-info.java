/**
 * Anwendungsschicht des ai-service: der Use-Case und die Pipeline-Schritte.
 *
 * <ul>
 *   <li>{@link de.werkmundo.ai.application.AnalyzeVoiceInputUseCase} – Ablauf
 *       Call 1 -> Call 2 -> Ergebnis an die Process Engine (asynchron)</li>
 *   <li><b>LLM-Call 1 (Extraktion):</b> {@link de.werkmundo.ai.application.LlmCall1Generator}
 *       mit {@link de.werkmundo.ai.application.Call1PromptBuilder} und
 *       {@link de.werkmundo.ai.application.ErgebnisKiParser}</li>
 *   <li><b>LLM-Call 2 (Produktauswahl):</b> {@link de.werkmundo.ai.application.Call2Selector}
 *       mit {@link de.werkmundo.ai.application.Call2PromptBuilder} und
 *       {@link de.werkmundo.ai.application.Call2AnswerParser}</li>
 *   <li>{@link de.werkmundo.ai.application.StubResultGenerator} – Fallback ohne LLM</li>
 *   <li>{@code port/} – Interfaces zu externen Systemen; die Implementierungen liegen in
 *       {@code infrastructure}</li>
 * </ul>
 *
 * <p><b>Datenschutz-Constraint:</b> Preise werden der KI nie uebergeben.
 */
package de.werkmundo.ai.application;
