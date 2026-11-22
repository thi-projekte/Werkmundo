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
import jakarta.ws.rs.WebApplicationException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
            insertInvoice(connection, invoice, null);
            insertPositions(connection, invoice);
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Rechnung konnte nicht gespeichert werden", exception);
        }
    }

    @Override
    @Transactional
    public Invoice insertOrGet(Invoice invoice, UUID idempotencyKey) {
        Objects.requireNonNull(invoice, "invoice darf nicht fehlen");
        Objects.requireNonNull(
                idempotencyKey, "idempotencyKey darf nicht fehlen");

        try (Connection connection = dataSource.get().getConnection()) {
            boolean inserted = insertInvoice(
                    connection, invoice, idempotencyKey);

            if (inserted) {
                insertPositions(connection, invoice);
                return invoice;
            }

            Invoice existing = findByIdempotencyKey(
                    connection, invoice.ownerId(), idempotencyKey)
                    .orElseThrow(() -> new IllegalStateException(
                            "Die bereits erstellte Rechnung wurde nicht gefunden"));

            if (!existing.offerBusinessKey().equals(invoice.offerBusinessKey())) {
                throw new WebApplicationException(
                        "Der Idempotenzschlüssel wurde bereits "
                                + "für ein anderes Angebot verwendet",
                        409);
            }

            return existing;
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Rechnung konnte nicht gespeichert oder geladen werden",
                    exception);
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
    public Optional<Invoice> findByIdempotencyKey(
            String ownerId, UUID idempotencyKey) {

        Objects.requireNonNull(ownerId, "ownerId darf nicht fehlen");
        Objects.requireNonNull(
                idempotencyKey, "idempotencyKey darf nicht fehlen");

        try (Connection connection = dataSource.get().getConnection()) {
            return findByIdempotencyKey(
                    connection, ownerId, idempotencyKey);
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Rechnung konnte nicht anhand des "
                            + "Idempotenzschlüssels geladen werden",
                    exception);
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

    private Optional<Invoice> findByIdempotencyKey(
            Connection connection,
            String ownerId,
            UUID idempotencyKey) throws SQLException {

        String sql = """
                SELECT *
                FROM invoices
                WHERE owner_id = ? AND idempotency_key = ?
                """;

        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, ownerId);
            statement.setObject(2, idempotencyKey);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }

                return Optional.of(readInvoice(connection, result));
            }
        }
    }

    private boolean insertInvoice(
            Connection connection,
            Invoice invoice,
            UUID idempotencyKey) throws SQLException {

        String sql = """
                INSERT INTO invoices (
                    id, owner_id, offer_business_key, rechnungsnummer,
                    kunde_vorname, kunde_nachname, kunde_email,
                    kunde_strasse, kunde_hausnummer, kunde_plz, kunde_ort,
                    gesamt_preis, created_at, updated_at, idempotency_key
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT ON CONSTRAINT invoices_owner_idempotency_unique
                DO NOTHING
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
            statement.setObject(15, idempotencyKey);

            return statement.executeUpdate() == 1;
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