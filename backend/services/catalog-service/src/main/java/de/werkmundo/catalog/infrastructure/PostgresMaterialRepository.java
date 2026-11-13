package de.werkmundo.catalog.infrastructure;

import de.werkmundo.catalog.domain.Material;
import de.werkmundo.catalog.domain.MaterialRepository;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.sql.SQLException;
import java.util.List;

@ApplicationScoped
public class PostgresMaterialRepository implements MaterialRepository {
    private final AgroalDataSource dataSource;

    public PostgresMaterialRepository(AgroalDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    @Transactional
    public void upsert(String ownerId, List<Material> materials) {
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
        } catch (SQLException e) {
            throw new IllegalStateException("Katalog konnte nicht gespeichert werden", e);
        }
    }
}
