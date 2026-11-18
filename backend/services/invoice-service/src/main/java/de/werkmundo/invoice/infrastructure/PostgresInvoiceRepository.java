package de.werkmundo.invoice.infrastructure;

import de.werkmundo.invoice.domain.Invoice;
import de.werkmundo.invoice.domain.InvoiceCustomerSnapshot;
import de.werkmundo.invoice.domain.InvoicePosition;
import de.werkmundo.invoice.domain.InvoicePositionType;
import de.werkmundo.invoice.domain.InvoiceRepository;
import io.agroal.api.AgroalDataSource;
import io.quarkus.arc.lookup.LookupIfProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@LookupIfProperty(
        name = "document.storage.postgres.enabled",
        stringValue = "true"
)
public class PostgresInvoiceRepository implements InvoiceRepository {

    private final Instance<AgroalDataSource> dataSource;

    @Inject
    public PostgresInvoiceRepository(Instance<AgroalDataSource> dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    @Transactional
    public void insert(Invoice invoice) {
        try (Connection connection = dataSource.get().getConnection()) {
            insertInvoice(connection, invoice);
            insertPositions(connection, invoice);
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Rechnung konnte nicht gespeichert werden", exception);
        }
    }

    @Override
    public Optional<Invoice> findById(UUID id, String ownerId) {
        String sql = """
                SELECT *
                FROM invoices
                WHERE id = ? AND owner_id = ?
                """;

        try (Connection connection = dataSource.get().getConnection();
             var statement = connection.prepareStatement(sql)) {

            statement.setObject(1, id);
            statement.setString(2, ownerId);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }

                return Optional.of(readInvoice(connection, result));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Rechnung konnte nicht geladen werden", exception);
        }
    }

    @Override
    public List<Invoice> findByOwner(String ownerId) {
        String sql = """
                SELECT *
                FROM invoices
                WHERE owner_id = ?
                ORDER BY created_at DESC, id DESC
                """;

        try (Connection connection = dataSource.get().getConnection();
             var statement = connection.prepareStatement(sql)) {

            statement.setString(1, ownerId);

            try (ResultSet result = statement.executeQuery()) {
                List<Invoice> invoices = new ArrayList<>();

                while (result.next()) {
                    invoices.add(readInvoice(connection, result));
                }

                return invoices;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Rechnungen konnten nicht geladen werden", exception);
        }
    }

    private void insertInvoice(Connection connection, Invoice invoice)
            throws SQLException {

        String sql = """
                INSERT INTO invoices (
                    id, owner_id, offer_business_key, rechnungsnummer,
                    kunde_vorname, kunde_nachname, kunde_email,
                    kunde_strasse, kunde_hausnummer, kunde_plz, kunde_ort,
                    gesamt_preis, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (var statement = connection.prepareStatement(sql)) {
            var customer = invoice.kundendaten();

            statement.setObject(1, invoice.id());
            statement.setString(2, invoice.ownerId());
            statement.setString(3, invoice.offerBusinessKey());
            statement.setString(4, invoice.rechnungsnummer());
            statement.setString(5, customer.vorname());
            statement.setString(6, customer.nachname());
            statement.setString(7, customer.email());
            statement.setString(8, customer.strasse());
            statement.setString(9, customer.hausnummer());
            statement.setString(10, customer.plz());
            statement.setString(11, customer.ort());
            statement.setBigDecimal(12, invoice.gesamtPreis());
            statement.setObject(
                    13, invoice.createdAt().atOffset(ZoneOffset.UTC));
            statement.setObject(
                    14, invoice.updatedAt().atOffset(ZoneOffset.UTC));

            statement.executeUpdate();
        }
    }

    private void insertPositions(Connection connection, Invoice invoice)
            throws SQLException {

        String sql = """
                INSERT INTO invoice_positions (
                    invoice_id, reihenfolge, hersteller, bezeichnung,
                    menge, einheit, katalog_produkt_id,
                    einzel_preis, positions_preis, position_type
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (var statement = connection.prepareStatement(sql)) {
            for (InvoicePosition position : invoice.positions()) {
                statement.setObject(1, invoice.id());
                statement.setInt(2, position.reihenfolge());
                statement.setString(3, position.hersteller());
                statement.setString(4, position.bezeichnung());
                statement.setBigDecimal(5, position.menge());
                statement.setString(6, position.einheit());
                statement.setString(7, position.katalogProduktId());
                statement.setBigDecimal(8, position.einzelPreis());
                statement.setBigDecimal(9, position.positionsPreis());
                statement.setString(10, position.type().name());
                statement.addBatch();
            }

            statement.executeBatch();
        }
    }

    private Invoice readInvoice(Connection connection, ResultSet row)
            throws SQLException {

        UUID id = row.getObject("id", UUID.class);

        var customer = new InvoiceCustomerSnapshot(
                row.getString("kunde_vorname"),
                row.getString("kunde_nachname"),
                row.getString("kunde_email"),
                row.getString("kunde_strasse"),
                row.getString("kunde_hausnummer"),
                row.getString("kunde_plz"),
                row.getString("kunde_ort")
        );

        return new Invoice(
                id,
                row.getString("owner_id"),
                row.getString("offer_business_key"),
                row.getString("rechnungsnummer"),
                customer,
                row.getBigDecimal("gesamt_preis"),
                row.getObject("created_at", OffsetDateTime.class).toInstant(),
                row.getObject("updated_at", OffsetDateTime.class).toInstant(),
                readPositions(connection, id)
        );
    }

    private List<InvoicePosition> readPositions(
            Connection connection, UUID invoiceId) throws SQLException {

        String sql = """
                SELECT *
                FROM invoice_positions
                WHERE invoice_id = ?
                ORDER BY reihenfolge
                """;

        try (var statement = connection.prepareStatement(sql)) {
            statement.setObject(1, invoiceId);

            try (ResultSet result = statement.executeQuery()) {
                List<InvoicePosition> positions = new ArrayList<>();

                while (result.next()) {
                    positions.add(new InvoicePosition(
                            result.getString("hersteller"),
                            result.getString("bezeichnung"),
                            result.getBigDecimal("menge"),
                            result.getString("einheit"),
                            result.getString("katalog_produkt_id"),
                            result.getBigDecimal("einzel_preis"),
                            result.getBigDecimal("positions_preis"),
                            result.getInt("reihenfolge"),
                            InvoicePositionType.valueOf(
                                    result.getString("position_type"))
                    ));
                }

                return positions;
            }
        }
    }
}