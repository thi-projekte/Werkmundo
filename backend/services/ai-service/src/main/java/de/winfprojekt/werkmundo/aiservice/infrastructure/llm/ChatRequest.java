package de.winfprojekt.werkmundo.aiservice.infrastructure.llm;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Request-Body der OpenAI-kompatiblen Chat-Completions-API ({@code POST /chat/completions}).
 * Bewusst ohne {@code response_format}: die Absicherung ist das robuste Parsen in der
 * application-Schicht.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatRequest(String model, List<Message> messages, Double temperature) {

    /** @param role {@code "system"} oder {@code "user"} */
    public record Message(String role, String content) {}
}
