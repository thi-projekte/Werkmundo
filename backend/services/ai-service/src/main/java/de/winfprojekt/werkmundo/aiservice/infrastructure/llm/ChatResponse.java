package de.winfprojekt.werkmundo.aiservice.infrastructure.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Response-Body der Chat-Completions-API. Gelesen wird nur der Text der ersten Choice und die Token-Zahlen. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatResponse(List<Choice> choices, Usage usage) {

    /** Textinhalt der ersten Choice oder {@code null}. */
    public String firstContent() {
        if (choices == null || choices.isEmpty() || choices.get(0) == null || choices.get(0).message() == null) {
            return null;
        }
        return choices.get(0).message().content();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(Message message) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(String role, String content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Usage(
            @JsonProperty("prompt_tokens") Integer promptTokens,
            @JsonProperty("completion_tokens") Integer completionTokens,
            @JsonProperty("total_tokens") Integer totalTokens) {}
}
