package de.winfprojekt.werkmundo.aiservice.domain;

/**
 * Welche Pipeline-Variante ausgefuehrt wird. Die Process Engine schickt kein explizites
 * {@code typ}-Feld; der Typ wird in {@link AnalyseAuftrag#of} aus den vorhandenen Feldern
 * abgeleitet (offene Abstimmung mit Squad Prozesse: siehe {@code docs/OFFENE-PUNKTE.md}).
 */
public enum ProcessType {

    /** Neues Angebot aus {@code sprachschnipsel} + {@code vorlage}. */
    ERSTANGEBOT,

    /** Ueberarbeitung bestehender {@code strukturierteAngebotspositionen} per {@code korrekturschnipsel}. */
    KORREKTUR
}
