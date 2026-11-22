package de.werkmundo.invoice.infrastructure;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class InvoiceNumberIssuer {
    private final Instance<AgroalDataSource> dataSource;
    private final InvoiceNumberGenerator generator;

    @Inject
    public InvoiceNumberIssuer(Instance<AgroalDataSource> dataSource, InvoiceNumberGenerator generator) {
        this.dataSource = dataSource;
        this.generator = generator;
    }

    @Transactional
    public Optional<String> issue(UUID invoiceId, String ownerId) {
        Objects.requireNonNull(invoiceId, "invoiceId");
        if (ownerId == null || ownerId.isBlank()) {
            throw new IllegalArgumentException("Owner is required");
        }
        try (Connection connection = dataSource.get().getConnection();
             PreparedStatement find = connection.prepareStatement(
                     "SELECT rechnungsnummer FROM invoices WHERE id = ? AND owner_id = ? FOR UPDATE")) {
            find.setObject(1, invoiceId);
            find.setString(2, ownerId);
            try (ResultSet result = find.executeQuery()) {
                if (!result.next()) return Optional.empty();
                String existing = result.getString(1);
                if (existing != null) return Optional.of(existing);
            }

            String number = generator.nextNumber(connection);
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE invoices SET rechnungsnummer = ?, updated_at = CURRENT_TIMESTAMP " +
                    "WHERE id = ? AND owner_id = ? AND rechnungsnummer IS NULL")) {
                update.setString(1, number);
                update.setObject(2, invoiceId);
                update.setString(3, ownerId);
                if (update.executeUpdate() != 1) {
                    throw new IllegalStateException("Invoice numbering update failed");
                }
            }
            return Optional.of(number);
        } catch (SQLException exception) {
            throw new IllegalStateException("Invoice numbering failed", exception);
        }
    }
}
