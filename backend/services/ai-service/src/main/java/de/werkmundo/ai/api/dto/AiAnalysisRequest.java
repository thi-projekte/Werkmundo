package de.werkmundo.ai.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.werkmundo.ai.domain.AnalyseAuftrag;
import de.werkmundo.ai.domain.Angebotspositionen;
import de.werkmundo.ai.domain.Vorlage;

/**
 * Payload von {@code POST /ai/process}, wie ihn die Process Engine schickt.
 * JSON-Schema: {@code contracts/ai/AiAnalysisRequest.json}.
 *
 * <p>Erstangebot: {@code vorlage} + {@code sprachschnipsel}. Korrektur:
 * {@code strukturierteAngebotspositionen} + {@code korrekturschnipsel}. Kundendaten werden
 * nicht uebergeben. Zusaetzliche Felder (z.B. {@code prompt}) werden ignoriert.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiAnalysisRequest(
        String businessKey,
        Vorlage vorlage,
        String sprachschnipsel,
        Angebotspositionen strukturierteAngebotspositionen,
        String korrekturschnipsel
) {

    /** Uebersetzt in den Domain-Auftrag; wirft {@code UngueltigerAuftragException}, wenn unvollstaendig. */
    public AnalyseAuftrag toAuftrag(String handwerkerId) {
        return AnalyseAuftrag.of(businessKey, handwerkerId, vorlage, sprachschnipsel,
                strukturierteAngebotspositionen, korrekturschnipsel);
    }
}
