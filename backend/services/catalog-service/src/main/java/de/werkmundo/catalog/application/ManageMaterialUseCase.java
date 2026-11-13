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
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@ApplicationScoped
public class ManageMaterialUseCase {
    private static final Set<String> REQUIRED_COLUMNS = Set.of("articleNumber", "name", "unit", "price");
    private static final CSVFormat CSV = CSVFormat.DEFAULT.builder()
            .setDelimiter(';').setHeader().setSkipHeaderRecord(true).setTrim(true)
            .setDuplicateHeaderMode(DuplicateHeaderMode.DISALLOW).get();

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
                if (!parser.getHeaderNames().containsAll(REQUIRED_COLUMNS)) {
                    throw new InvalidCsvException("Pflichtspalten fehlen (articleNumber, name, unit, price)");
                }
                for (CSVRecord row : parser) {
                    Material material = parse(row);
                    if (!articleNumbers.add(material.articleNumber())) {
                        throw new InvalidCsvException("Doppelte Artikelnummer in Datensatz " + row.getRecordNumber());
                    }
                    materials.add(material);
                }
            }
        } catch (IOException | UncheckedIOException | IllegalArgumentException e) {
            throw new InvalidCsvException("CSV konnte nicht gelesen werden");
        }
        if (materials.isEmpty()) {
            throw new InvalidCsvException("Die CSV enthält keine Artikel");
        }
        // Keep the lock order consistent for simultaneous imports with overlapping articles.
        materials.sort(Comparator.comparing(Material::articleNumber));
        repository.upsert(ownerId, materials);
        return materials.size();
    }

    private Material parse(CSVRecord row) {
        try {
            if (!row.isConsistent() || row.stream().anyMatch(value -> value.indexOf(0) >= 0)) {
                throw new IllegalArgumentException();
            }
            String articleNumber = row.get("articleNumber");
            String name = row.get("name");
            String unit = row.get("unit");
            String priceText = row.get("price").replace(',', '.');
            String currency = optional(row, "currency");
            if (currency.isBlank()) {
                currency = "EUR";
            }
            if (articleNumber.isBlank() || articleNumber.length() > 255 || name.isBlank() || unit.isBlank()
                    || !priceText.matches("[0-9]{1,18}(\\.[0-9]{1,12})?") || !currency.matches("[A-Z]{3}")) {
                throw new IllegalArgumentException();
            }
            return new Material(articleNumber, name, optional(row, "manufacturer"),
                    optional(row, "description"), optional(row, "category"), unit,
                    new BigDecimal(priceText), currency);
        } catch (IllegalArgumentException e) {
            throw new InvalidCsvException("Ungültige Artikeldaten in Datensatz " + row.getRecordNumber());
        }
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
