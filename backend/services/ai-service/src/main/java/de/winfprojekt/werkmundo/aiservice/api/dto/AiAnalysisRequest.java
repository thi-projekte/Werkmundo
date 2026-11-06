package de.winfprojekt.werkmundo.aiservice.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.winfprojekt.werkmundo.aiservice.domain.AnalyseAuftrag;
import de.winfprojekt.werkmundo.aiservice.domain.Angebotspositionen;
import de.winfprojekt.werkmundo.aiservice.domain.Vorlage;

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
