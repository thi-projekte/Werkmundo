package de.winfprojekt.werkmundo.aiservice.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extrahiert aus der Antwort von LLM-Call 2 die gewaehlte articleNumber oder
 * {@link #KEIN_TREFFER}. Toleriert Markdown-Fences, fehlendes JSON (Regex-Fallback) und
 * verschiedene "kein Treffer"-Schreibweisen. Ob die Nummer in der Kandidatenliste steht,
 * prueft der {@link Call2Selector}.
 */
@ApplicationScoped
public class Call2AnswerParser {

    public static final String KEIN_TREFFER = "KEIN_TREFFER";

    private static final Pattern FENCE_START = Pattern.compile("(?s)^\\s*```(?:json)?\\s*");
    private static final Pattern FENCE_END = Pattern.compile("(?s)\\s*```\\s*$");
    /** Artikelnummern: Buchstaben-Segment(-Segment)*, z.B. ELE-3004, SCH-JUNG-SD, INS-KAI-GD2-25. */
    private static final Pattern ARTICLE = Pattern.compile("[A-Z]{2,6}(?:-[A-Z0-9]+)+");
    private static final Pattern KEIN_TREFFER_IM_TEXT =
            Pattern.compile("kein[_\\s-]?treffer", Pattern.CASE_INSENSITIVE);
    private static final Pattern KEIN_TREFFER_WERT =
            Pattern.compile("^(kein[_\\s-]?treffer|none|null|keiner?)$", Pattern.CASE_INSENSITIVE);

    private final ObjectMapper objectMapper;

    public Call2AnswerParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String parsePick(String rawOutput) {
        String t = rawOutput == null ? "" : rawOutput.trim();
        t = FENCE_START.matcher(t).replaceAll("");
        t = FENCE_END.matcher(t).replaceAll("").trim();

        String art = null;
        try {
            JsonNode o = objectMapper.readTree(t);
            if (o != null && o.isObject()) {
                JsonNode an = o.has("articleNumber") ? o.get("articleNumber") : o.get("article_number");
                if (an != null && !an.isNull()) {
                    art = an.asText();
                }
            }
        } catch (Exception keinJson) {
            Matcher m = ARTICLE.matcher(t);
            if (m.find()) {
                art = m.group();
            } else if (KEIN_TREFFER_IM_TEXT.matcher(t).find()) {
                art = KEIN_TREFFER;
            }
        }

        if (art == null || KEIN_TREFFER_WERT.matcher(art.trim()).matches()) {
            return KEIN_TREFFER;
        }
        Matcher norm = ARTICLE.matcher(art.trim().toUpperCase());
        return norm.find() ? norm.group() : art.trim();
    }
}
