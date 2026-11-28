package de.werkmundo.catalog.application;

import de.werkmundo.catalog.domain.Material;
import de.werkmundo.catalog.domain.MaterialRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.DuplicateHeaderMode;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@ApplicationScoped
public class ManageMaterialUseCase {
    private static final List<String> REQUIRED_COLUMNS = List.of("articleNumber", "name", "unit", "price");
    private static final CSVFormat CSV = CSVFormat.DEFAULT.builder()
            .setDelimiter(';').setHeader().setSkipHeaderRecord(true).setTrim(true)
            .setAllowMissingColumnNames(true).setDuplicateHeaderMode(DuplicateHeaderMode.ALLOW_ALL).get();

    private final MaterialRepository repository;

    public ManageMaterialUseCase(MaterialRepository repository) {
        this.repository = repository;
    }

    public int importCsv(String ownerId, Path file) {
        List<Material> materials = new ArrayList<>();
        Set<String> articleNumbers = new TreeSet<>();
        try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            reader.mark(1);
            if (reader.read() != '\ufeff') {
                reader.reset();
            }
            try (var parser = CSV.parse(reader)) {
                Set<String> headers = new HashSet<>();
                for (String header : parser.getHeaderNames()) {
                    if (header.isBlank()) {
                        throw new InvalidCsvException("Kopfzeile: Spaltennamen dürfen nicht leer sein");
                    }
                    if (!headers.add(header)) {
                        throw new InvalidCsvException("Kopfzeile: Doppelte Spalte '" + header + "'");
                    }
                }
                List<String> missing = REQUIRED_COLUMNS.stream().filter(column -> !headers.contains(column)).toList();
                if (!missing.isEmpty()) {
                    throw new InvalidCsvException("Pflichtspalten fehlen: " + String.join(", ", missing));
                }
                try {
                    for (CSVRecord row : parser) {
                        long line = parser.getCurrentLineNumber();
                        Material material = parse(row, line);
                        if (!articleNumbers.add(material.articleNumber())) {
                            throw invalid(line, "Doppelte Artikelnummer '" + material.articleNumber() + "'");
                        }
                        materials.add(material);
                    }
                } catch (UncheckedIOException e) {
                    throw invalid(parser.getCurrentLineNumber(), "CSV-Syntax oder UTF-8-Kodierung ungültig");
                }
            }
        } catch (IOException | UncheckedIOException | IllegalArgumentException e) {
            throw new InvalidCsvException("CSV konnte nicht gelesen werden: UTF-8 und Kopfzeile mit eindeutigen, nicht leeren Spaltennamen prüfen");
        }
        if (materials.isEmpty()) {
            throw new InvalidCsvException("Die CSV enthält keine Artikel");
        }
        repository.upsert(ownerId, materials);
        return materials.size();
    }

    private Material parse(CSVRecord row, long line) {
        if (!row.isConsistent()) {
            throw invalid(line, "Anzahl der Werte stimmt nicht mit der Kopfzeile überein");
        }
        if (row.stream().anyMatch(value -> value.indexOf(0) >= 0)) {
            throw invalid(line, "Nullzeichen sind nicht erlaubt");
        }
        String articleNumber = required(row, "articleNumber", line);
        if (articleNumber.length() > 255) {
            throw invalid(line, "articleNumber darf höchstens 255 Zeichen enthalten");
        }
        String name = required(row, "name", line);
        String unit = required(row, "unit", line);
        String priceText = required(row, "price", line).replace(',', '.');
        if (!priceText.matches("[0-9]{1,18}(\\.[0-9]{1,12})?")) {
            throw invalid(line, "price muss eine nicht negative Zahl mit höchstens 18 Vor- und 12 Nachkommastellen sein; "
                    + "Dezimalpunkt oder Dezimalkomma, keine Tausendertrennzeichen");
        }
        String currency = optional(row, "currency");
        if (currency.isBlank()) {
            currency = "EUR";
        }
        if (!currency.matches("[A-Z]{3}")) {
            throw invalid(line, "currency muss aus drei Großbuchstaben bestehen (z. B. EUR)");
        }
        return new Material(articleNumber, name, optional(row, "manufacturer"),
                optional(row, "description"), optional(row, "category"), unit,
                new BigDecimal(priceText), currency);
    }

    private String required(CSVRecord row, String column, long line) {
        String value = row.get(column);
        if (value.isBlank()) {
            throw invalid(line, "Pflichtwert '" + column + "' fehlt");
        }
        return value;
    }

    private InvalidCsvException invalid(long line, String message) {
        return new InvalidCsvException("Zeile " + line + ": " + message);
    }

    private String optional(CSVRecord row, String column) {
        return row.isMapped(column) ? row.get(column) : "";
    }

    public static final class InvalidCsvException extends RuntimeException {
        public InvalidCsvException(String message) {
            super(message);
        }
    }
}
