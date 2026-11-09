/**
 * Technische Adapter des ai-service. Sie implementieren die Ports aus
 * {@code application.port} und kapseln alle ausgehenden HTTP-Aufrufe.
 *
 * <ul>
 *   <li>{@code llm/} – MegaLLM (OpenAI-kompatible Chat-Completions-API), Retry und Modell-Fallback</li>
 *   <li>{@code catalog/} – Produktsuche: Mock-Katalog (lokal, Default) oder catalog-service (REST)</li>
 *   <li>{@code processengine/} – Message-Korrelation {@code ergebnisKI} an die Process Engine (CIB seven / Camunda 7 REST)</li>
 *   <li>{@code auth/} – technisches Keycloak-Token (client_credentials) fuer ausgehende Aufrufe, per Env aktivierbar</li>
 * </ul>
 *
 * <p>URLs und Schalter kommen aus {@code application.properties} (alle per Env-Variable
 * ueberschreibbar, siehe {@code .env.example}).
 */
package de.winfprojekt.werkmundo.aiservice.infrastructure;
