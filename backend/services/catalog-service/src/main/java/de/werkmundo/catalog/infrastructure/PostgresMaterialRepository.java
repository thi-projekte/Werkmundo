package de.werkmundo.catalog.infrastructure;

import de.werkmundo.catalog.domain.CatalogItem;
import de.werkmundo.catalog.domain.CatalogReader;
import de.werkmundo.catalog.domain.Material;
import de.werkmundo.catalog.domain.MaterialRepository;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PostgresMaterialRepository implements MaterialRepository, CatalogReader {
    private final AgroalDataSource dataSource;

    public PostgresMaterialRepository(AgroalDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    @Transactional
    public void replace(String ownerId, List<Material> materials) {
        String sql = """
                INSERT INTO material (owner_id, article_number, name, manufacturer, description,
                                      category, unit, price, currency)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (owner_id, article_number) DO UPDATE SET
                    name = EXCLUDED.name, manufacturer = EXCLUDED.manufacturer,
                    description = EXCLUDED.description, category = EXCLUDED.category,
                    unit = EXCLUDED.unit, price = EXCLUDED.price, currency = EXCLUDED.currency
                """;
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            // Serialize imports per owner, including the first import; readers retain the committed catalog.
            try (var lock = connection.prepareStatement("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))")) {
                lock.setString(1, ownerId);
                lock.execute();
            }
            for (Material material : materials) {
                statement.setString(1, ownerId);
                statement.setString(2, material.articleNumber());
                statement.setString(3, material.name());
                statement.setString(4, material.manufacturer());
                statement.setString(5, material.description());
                statement.setString(6, material.category());
                statement.setString(7, material.unit());
                statement.setBigDecimal(8, material.price());
                statement.setString(9, material.currency());
                statement.addBatch();
            }
            statement.executeBatch();
            try (var delete = connection.prepareStatement(
                    "DELETE FROM material WHERE owner_id = ? AND NOT (article_number = ANY (?))")) {
                delete.setString(1, ownerId);
                var articles = connection.createArrayOf("text", materials.stream().map(Material::articleNumber).toArray(String[]::new));
                try {
                    delete.setArray(2, articles);
                    delete.executeUpdate();
                } finally {
                    articles.free();
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Katalog konnte nicht gespeichert werden", e);
        }
    }

    @Override
    public List<CatalogItem> search(String ownerId, String query, int limit) {
        String sql = """
                SELECT * FROM material WHERE owner_id = ?
                AND strpos(lower(concat_ws(' ', article_number, name, manufacturer, description, category)), lower(?)) > 0
                ORDER BY (lower(article_number) = lower(?)) DESC, article_number LIMIT ?
                """;
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, ownerId);
            statement.setString(2, query);
            statement.setString(3, query);
            statement.setInt(4, limit);
            try (var rows = statement.executeQuery()) {
                List<CatalogItem> result = new ArrayList<>();
                while (rows.next()) result.add(item(rows));
                return result;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Katalog konnte nicht durchsucht werden", e);
        }
    }

    @Override
    public Optional<CatalogItem> find(String ownerId, UUID id) {
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement("SELECT * FROM material WHERE owner_id = ? AND id = ?")) {
            statement.setString(1, ownerId);
            statement.setObject(2, id);
            try (var rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(item(rows)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Artikel konnte nicht geladen werden", e);
        }
    }

    @Override
    public long count(String ownerId) {
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement("SELECT count(*) FROM material WHERE owner_id = ?")) {
            statement.setString(1, ownerId);
            try (var rows = statement.executeQuery()) {
                rows.next();
                return rows.getLong(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Katalogstatus konnte nicht geladen werden", e);
        }
    }

    private CatalogItem item(ResultSet row) throws SQLException {
        return new CatalogItem(row.getObject("id", UUID.class), row.getString("article_number"),
                row.getString("name"), row.getString("manufacturer"), row.getString("description"),
                row.getString("category"), row.getString("unit"), row.getBigDecimal("price"), row.getString("currency"));
    }
}
