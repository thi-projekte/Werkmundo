package de.winfprojekt.werkmundo.aiservice.infrastructure.llm;

import de.winfprojekt.werkmundo.aiservice.application.LlmException;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Retry und Modell-Fallback des MegaLLM-Adapters (Backoff im Test auf 1 ms). */
class MegaLlmAdapterTest {

    private static Response ok(String content) {
        ChatResponse body = new ChatResponse(
                List.of(new ChatResponse.Choice(new ChatResponse.Message("assistant", content))), null);
        Response resp = Mockito.mock(Response.class);
        Mockito.when(resp.getStatus()).thenReturn(200);
        Mockito.when(resp.readEntity(ChatResponse.class)).thenReturn(body);
        return resp;
    }

    private static Response status(int code) {
        Response resp = Mockito.mock(Response.class);
        Mockito.when(resp.getStatus()).thenReturn(code);
        Mockito.when(resp.readEntity(String.class)).thenReturn("error " + code);
        return resp;
    }

    private static MegaLlmAdapter adapter(MegaLlmClient client, String model, String fallback) {
        return new MegaLlmAdapter(client, Optional.of("test-key"), model, Optional.ofNullable(fallback), 1);
    }

    @Test
    void ohneKey_nichtKonfiguriert() {
        MegaLlmAdapter a = new MegaLlmAdapter(Mockito.mock(MegaLlmClient.class), Optional.empty(),
                "m", Optional.empty(), 1);
        assertFalse(a.isConfigured());
        assertThrows(LlmException.class, () -> a.complete("sys", "user"));
    }

    @Test
    void primaerFehlschlag_nutztFallbackModell() {
        List<String> models = new ArrayList<>();
        MegaLlmClient client = Mockito.mock(MegaLlmClient.class);
        Mockito.when(client.complete(Mockito.anyString(), Mockito.any(ChatRequest.class))).thenAnswer(inv -> {
            ChatRequest req = inv.getArgument(1);
            models.add(req.model());
            return "primaer".equals(req.model()) ? ok("") : ok("OK vom Fallback");
        });

        assertEquals("OK vom Fallback", adapter(client, "primaer", "fallback").complete("sys", "user"));
        assertEquals(List.of("primaer", "fallback"), models);
    }

    @Test
    void primaer503Exception_nutztFallback_nachRetries() {
        List<String> models = new ArrayList<>();
        MegaLlmClient client = Mockito.mock(MegaLlmClient.class);
        Mockito.when(client.complete(Mockito.anyString(), Mockito.any(ChatRequest.class))).thenAnswer(inv -> {
            ChatRequest req = inv.getArgument(1);
            models.add(req.model());
            if ("primaer".equals(req.model())) {
                throw new WebApplicationException("Service Unavailable", 503);
            }
            return ok("OK vom Fallback");
        });

        assertEquals("OK vom Fallback", adapter(client, "primaer", "fallback").complete("sys", "user"));
        // 1 Versuch + 3 Retries auf dem Primaermodell, dann 1x Fallback
        assertEquals(MegaLlmAdapter.MAX_RETRIES + 2, models.size());
        assertEquals("fallback", models.get(models.size() - 1));
    }

    @Test
    void transienter429_wirdWiederholt() {
        MegaLlmClient client = Mockito.mock(MegaLlmClient.class);
        Response tooMany = status(429);
        Response okResp = ok("OK");
        Mockito.when(client.complete(Mockito.anyString(), Mockito.any(ChatRequest.class)))
                .thenReturn(tooMany, okResp);

        assertEquals("OK", adapter(client, "primaer", null).complete("sys", "user"));
        Mockito.verify(client, Mockito.times(2)).complete(Mockito.anyString(), Mockito.any(ChatRequest.class));
    }

    @Test
    void http400_keinRetry() {
        MegaLlmClient client = Mockito.mock(MegaLlmClient.class);
        Response bad = status(400);
        Mockito.when(client.complete(Mockito.anyString(), Mockito.any(ChatRequest.class))).thenReturn(bad);

        assertThrows(LlmException.class, () -> adapter(client, "primaer", null).complete("sys", "user"));
        Mockito.verify(client, Mockito.times(1)).complete(Mockito.anyString(), Mockito.any(ChatRequest.class));
    }

    @Test
    void fallbackGleichPrimaer_keinZweiterVersuch() {
        MegaLlmClient client = Mockito.mock(MegaLlmClient.class);
        Response leer = ok("");
        Mockito.when(client.complete(Mockito.anyString(), Mockito.any(ChatRequest.class))).thenReturn(leer);

        assertThrows(LlmException.class, () -> adapter(client, "gleich", "gleich").complete("sys", "user"));
        Mockito.verify(client, Mockito.times(1)).complete(Mockito.anyString(), Mockito.any(ChatRequest.class));
    }
}
