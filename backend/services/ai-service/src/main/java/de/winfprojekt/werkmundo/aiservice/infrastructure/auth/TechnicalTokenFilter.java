package de.winfprojekt.werkmundo.aiservice.infrastructure.auth;

import io.quarkus.oidc.client.OidcClients;
import io.quarkus.oidc.client.Tokens;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;

/**
 * Haengt ein technisches Keycloak-Token ({@code client_credentials}, Client {@code ai-service})
 * an ausgehende Aufrufe zur Process Engine und zum catalog-service.
 *
 * <p>Nur aktiv mit {@code AI_TECHNICAL_TOKEN_ENABLED=true}. Lokal (Default) passiert nichts -
 * dann muss weder Keycloak laufen noch ein Client-Secret gesetzt sein. Das Token wird erst beim
 * ersten Aufruf geholt und bis kurz vor Ablauf wiederverwendet.
 */
@ApplicationScoped
public class TechnicalTokenFilter implements ClientRequestFilter {

    private final boolean enabled;
    private final Instance<OidcClients> oidcClients;
    private Tokens tokens;

    public TechnicalTokenFilter(@ConfigProperty(name = "ai.technical-token.enabled", defaultValue = "false")
                                boolean enabled,
                                Instance<OidcClients> oidcClients) {
        this.enabled = enabled;
        this.oidcClients = oidcClients;
    }

    @Override
    public void filter(ClientRequestContext request) {
        if (!enabled) {
            return;
        }
        request.getHeaders().putSingle(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken());
    }

    private synchronized String accessToken() {
        if (tokens == null || tokens.isAccessTokenWithinRefreshInterval() || tokens.isAccessTokenExpired()) {
            tokens = oidcClients.get().getClient().getTokens().await().atMost(Duration.ofSeconds(10));
        }
        return tokens.getAccessToken();
    }
}
