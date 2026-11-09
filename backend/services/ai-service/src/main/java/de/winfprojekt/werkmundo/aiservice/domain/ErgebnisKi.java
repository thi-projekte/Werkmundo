package de.winfprojekt.werkmundo.aiservice.domain;

import java.util.List;
import java.util.Objects;

/**
 * Ergebnis der KI-Verarbeitung. Geht als Prozessvariable {@code ergebnisKI} (Message gleichen
 * Namens, korreliert per {@code businessKey}) an die Process Engine.
 *
 * <pre>
 * {
 *   "strukturierteAngebotspositionen": { "leistungen": [...], "material": [...], "notizen": [...] },
 *   "korrekturvorschlaege": ["..."],
 *   "geschaetzteArbeitsdauerStunden": 2.5
 * }
 * </pre>
 *
 * <p>{@code geschaetzteArbeitsdauerStunden} ist nur gesetzt, wenn der Handwerker eine Dauer
 * ausgesprochen hat - die KI schaetzt nicht selbst. Stunden sind Aufwand, kein Preis.
 * JSON-Schema: {@code contracts/ai/AiAnalysisResult.json}.
 *
 * @param strukturierteAngebotspositionen erzeugte bzw. korrigierte Positionen
 * @param korrekturvorschlaege            Hinweise/Rueckfragen an den Handwerker
 * @param geschaetzteArbeitsdauerStunden  ausgesprochene Gesamtdauer in Stunden oder {@code null}
 */
public record ErgebnisKi(
        Angebotspositionen strukturierteAngebotspositionen,
        List<String> korrekturvorschlaege,
        Double geschaetzteArbeitsdauerStunden
) {

    public ErgebnisKi {
        korrekturvorschlaege = korrekturvorschlaege == null ? List.of() : korrekturvorschlaege.stream().filter(Objects::nonNull).toList();
    }

    /** Kopie mit ersetzten Angebotspositionen. */
    public ErgebnisKi withPositionen(Angebotspositionen positionen) {
        return new ErgebnisKi(positionen, korrekturvorschlaege, geschaetzteArbeitsdauerStunden);
    }
}
