package de.werkmundo.catalog;

import de.werkmundo.catalog.application.ManageMaterialUseCase;
import de.werkmundo.catalog.domain.Material;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ManageMaterialUseCaseTest {
    @TempDir Path directory;
    private final List<Material> saved = new ArrayList<>();
    private final ManageMaterialUseCase catalog = new ManageMaterialUseCase((owner, materials) -> {
        assertEquals("owner", owner);
        saved.addAll(materials);
    });
    private static final String HEADER = "articleNumber;name;unit;price\n";

    @Test
    void acceptsBomReorderedColumnsAndQuotedMultilineFields() throws IOException {
        assertEquals(1, upload("\ufeffprice;unit;name;articleNumber;description;currency\n"
                + "1,723; Stück ;\"Schalter; weiß\";001;\"Text mit \"\"Zitat\"\"\nzweite Zeile\";\n"));
        var material = saved.getFirst();
        assertEquals("001", material.articleNumber());
        assertEquals("Schalter; weiß", material.name());
        assertEquals("Stück", material.unit());
        assertEquals(new BigDecimal("1.723"), material.price());
        assertEquals("EUR", material.currency());
        assertEquals("", material.manufacturer());
        assertEquals("Text mit \"Zitat\"\nzweite Zeile", material.description());
    }

    @Test
    void acceptsExampleCsv() {
        assertEquals(2, catalog.importCsv("owner", Path.of("../../../frontend/public/catalog-example.csv")));
    }

    @Test
    void explainsMissingDuplicateAndEmptyHeaders() throws IOException {
        rejects("articleNumber;name\nA;Artikel\n", "Pflichtspalten fehlen: unit, price");
        rejects("articleNumber;name;unit;price;price\n", "Kopfzeile: Doppelte Spalte 'price'");
        rejects("articleNumber;name;unit;price;\n", "Kopfzeile");
        rejects("articleNumber,name,unit,price\nA,Artikel,Stück,1\n", "Pflichtspalten fehlen");
        rejects("", "Pflichtspalten fehlen");
        rejects(HEADER + "\n", "keine Artikel");
    }

    @Test
    void identifiesEveryMissingRequiredValue() throws IOException {
        for (int column = 0; column < 4; column++) {
            String[] values = {"A", "Artikel", "Stück", "1"};
            values[column] = " ";
            rejects(HEADER + String.join(";", values), "Zeile 2: Pflichtwert '"
                    + List.of("articleNumber", "name", "unit", "price").get(column) + "' fehlt");
        }
    }

    @Test
    void validatesPricesAndCurrency() throws IOException {
        for (String price : List.of("-1", "NaN", "1e3", "1.000,50", "1,234.50", "1.1234567890123", "1234567890123456789")) {
            rejects(HEADER + "A;Artikel;Stück;" + price, "Zeile 2: price");
        }
        for (String price : List.of("0", "1.723", "1,723", "999999999999999999.123456789012")) {
            assertEquals(1, upload(HEADER + "A;Artikel;Stück;" + price));
        }
        saved.clear();
        rejects("articleNumber;name;unit;price;currency\nA;Artikel;Stück;1;eur", "Zeile 2: currency");
    }

    @Test
    void rejectsDuplicatesWithoutSavingEarlierRows() throws IOException {
        rejects(HEADER + "A;Artikel;Stück;1\n A ;Anderer Artikel;Stück;2", "Zeile 3: Doppelte Artikelnummer 'A'");
    }

    @Test
    void reportsPhysicalLinesIncludingMultilineRecordsAndBlankLines() throws IOException {
        rejects(HEADER + "\nA;\"Artikel\nzweite Zeile\";Stück;1\nB;Artikel;Stück;-1", "Zeile 5: price");
    }

    @Test
    void rejectsMalformedRowsAndInvalidEncoding() throws IOException {
        rejects(HEADER + "A;Artikel;Stück", "Zeile 2: Anzahl");
        rejects(HEADER + "A;Artikel;Stück;1;extra", "Zeile 2: Anzahl");
        rejects(HEADER + "A;\"nicht geschlossen;Stück;1", "Zeile 2: CSV-Syntax");
        rejects(HEADER + "A;Art\u0000ikel;Stück;1", "Zeile 2: Nullzeichen");
        rejects(HEADER + "A".repeat(256) + ";Artikel;Stück;1", "Zeile 2: articleNumber");
        Path file = directory.resolve("invalid-utf8.csv");
        Files.write(file, new byte[]{(byte) 0xc3, 0x28});
        assertThrows(ManageMaterialUseCase.InvalidCsvException.class, () -> catalog.importCsv("owner", file));
        assertTrue(saved.isEmpty());
    }

    private int upload(String csv) throws IOException {
        Path file = directory.resolve("catalog.csv");
        Files.writeString(file, csv);
        return catalog.importCsv("owner", file);
    }

    private void rejects(String csv, String message) throws IOException {
        var error = assertThrows(ManageMaterialUseCase.InvalidCsvException.class, () -> upload(csv));
        assertTrue(error.getMessage().contains(message), error.getMessage());
        assertTrue(saved.isEmpty(), "Ungültige Dateien dürfen nicht an das Repository übergeben werden");
    }
}
