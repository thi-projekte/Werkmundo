package de.winfprojekt.werkmundo.aiservice.application;

import de.winfprojekt.werkmundo.aiservice.domain.Angebotspositionen;
import de.winfprojekt.werkmundo.aiservice.domain.ErgebnisKi;
import de.winfprojekt.werkmundo.aiservice.domain.Position;
import de.winfprojekt.werkmundo.aiservice.domain.ProcessType;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;

/**
 * Fest verdrahtetes {@link ErgebnisKi} als <b>Fallback</b>, wenn LLM-Call 1 nicht moeglich ist
 * (kein {@code MEGALLM_API_KEY} oder LLM-/Parse-Fehler). So laeuft der Service lokal auch ohne
 * Key und ein LLM-Ausfall bricht den Prozess nicht ab.
 *
 * <p>Inhalt: typisches Wallbox-Installationsangebot (haeufigster Demo-Fall) mit 4 Stunden
 * Arbeitszeit. Keine Preise.
 */
@ApplicationScoped
public class StubResultGenerator {

    static final double STUB_ARBEITSDAUER_STUNDEN = 4.0;

    public ErgebnisKi generate(ProcessType type) {
        return type == ProcessType.KORREKTUR ? korrektur() : erstangebot();
    }

    private static List<Position> wallboxMaterial() {
        return List.of(
                new Position("Wallbox", "Wallbox zum Laden von Elektrofahrzeugen.", 1.0, "Stück"),
                new Position("Starkstromkabel NYM-J 5x6mm²",
                        "Starkstrom-Zuleitung für die Versorgung der Wallbox.", 10.0, "m"),
                new Position("Netzwerkkabel Cat 7", "Datenleitung zur Anbindung der Wallbox.", 10.0, "m"),
                new Position("Leitungsschutzschalter C16",
                        "Leitungsschutzschalter zur Absicherung der Wallbox im Verteiler.", 1.0, "Stück"));
    }

    private static ErgebnisKi erstangebot() {
        return new ErgebnisKi(
                new Angebotspositionen(List.of(), wallboxMaterial(), List.of("Leitungslängen vor Ort prüfen.")),
                List.of("Wallbox-Modell und Ladeleistung mit dem Kunden abstimmen."),
                STUB_ARBEITSDAUER_STUNDEN);
    }

    private static ErgebnisKi korrektur() {
        List<Position> material = new ArrayList<>(wallboxMaterial());
        material.add(new Position("FI-Schutzschalter Typ B",
                "Laut Korrekturschnipsel ergänzt — allstromsensitiver Personenschutz für die Wallbox.",
                1.0, "Stück"));
        return new ErgebnisKi(
                new Angebotspositionen(List.of(), material,
                        List.of("Korrektur eingearbeitet — finale Mengen vor Ort prüfen.")),
                List.of("Zusätzlichen FI-Schutzschalter Typ B ergänzt — bitte bestätigen."),
                STUB_ARBEITSDAUER_STUNDEN);
    }
}
