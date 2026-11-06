package de.winfprojekt.werkmundo.aiservice.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Eine einzelne Leistungs- oder Materialposition.
 *
 * <p>Kommt als Element der {@code leistungen}- bzw. {@code material}-Listen vor: in der
 * {@link Vorlage} (Eingang Erstangebot), in den bestehenden {@link Angebotspositionen}
 * (Eingang Korrektur) und im {@link ErgebnisKi} (Ausgang).
 *
 * <p><b>Kein Preisfeld</b> (Datenschutz-Constraint). Unbekannte Felder wie {@code preis}
 * werden beim Einlesen verworfen.
 *
 * <p>{@code katalogProduktId} wird in LLM-Call 2 fuer Materialpositionen gesetzt (UUID-String
 * des catalog-service, im Mock eine Zahl als String). Der offer-service nutzt sie fuer den
 * Preis-Lookup. Bei Leistungen und Eingaben bleibt sie {@code null}.
 *
 * @param bezeichnung      Kurzname der Position
 * @param beschreibung     laengere Beschreibung
 * @param menge            numerische Menge oder {@code null}, wenn unbekannt
 * @param einheit          Einheit der Menge (z.B. "m", "Stk", "h")
 * @param katalogProduktId Katalog-ID des in Call 2 gewaehlten Produkts, sonst {@code null}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Position(
        String bezeichnung,
        String beschreibung,
        Double menge,
        String einheit,
        String katalogProduktId
) {

    /** Position ohne Katalog-ID (Eingaben und Ergebnis von LLM-Call 1). */
    public Position(String bezeichnung, String beschreibung, Double menge, String einheit) {
        this(bezeichnung, beschreibung, menge, einheit, null);
    }

    /** Kopie dieser Position mit gesetzter Katalog-ID (LLM-Call 2). */
    public Position withKatalogProduktId(String katalogProduktId) {
        return new Position(bezeichnung, beschreibung, menge, einheit, katalogProduktId);
    }
}
