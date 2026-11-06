package de.winfprojekt.werkmundo.aiservice.domain;

/**
 * Ein Katalogprodukt, das in LLM-Call 2 zur Auswahl steht. Bewusst <b>ohne Preis</b>.
 *
 * @param id            Katalog-ID (wird als {@code katalogProduktId} durchgereicht)
 * @param articleNumber Artikelnummer - das LLM waehlt ueber sie aus
 * @param name          Produktname
 * @param description   Produktbeschreibung
 * @param unit          Einheit
 * @param category      Kategorie (nur informativ, geht nicht in den Prompt)
 */
public record ProduktKandidat(
        String id,
        String articleNumber,
        String name,
        String description,
        String unit,
        String category
) {}
