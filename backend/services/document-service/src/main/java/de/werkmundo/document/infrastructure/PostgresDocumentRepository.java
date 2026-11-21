package de.werkmundo.document.infrastructure;

import de.werkmundo.document.domain.Document;
import de.werkmundo.document.domain.DocumentMetadata;
import de.werkmundo.document.domain.DocumentRepository;
import de.werkmundo.document.domain.DocumentType;
import io.agroal.api.AgroalDataSource;
import io.quarkus.arc.lookup.LookupIfProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@LookupIfProperty(name = "document.storage.postgres.enabled", stringValue = "true")
public class PostgresDocumentRepository implements DocumentRepository {
    private static final String METADATA_COLUMNS = "id, reference_id, document_type, file_name";
    private static final String SELECT_METADATA = "SELECT " + METADATA_COLUMNS + " FROM documents";
    private static final String ORDER_NEWEST = " ORDER BY created_at DESC, id DESC";

    private final Instance<AgroalDataSource> dataSource;

    @Inject
    public PostgresDocumentRepository(Instance<AgroalDataSource> dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public String save(Document document) {
        String id = UUID.randomUUID().toString();
        String sql = """
                INSERT INTO documents
                    (id, owner_id, document_type, reference_id, customer_id, recipient_email, file_name, pdf_content)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (var connection = dataSource.get().getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            statement.setString(2, document.ownerId());
            statement.setString(3, document.type().name());
            statement.setString(4, document.referenceId());
            statement.setString(5, document.customerId());
            statement.setString(6, document.recipientEmail());
            statement.setString(7, document.fileName());
            statement.setBytes(8, document.pdfContent());
            statement.executeUpdate();
            return id;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not store document", exception);
        }
    }

    @Override
    public Optional<DocumentMetadata> findMetadataByIdAndOwner(String documentId, String ownerId) {
        return query(SELECT_METADATA + " WHERE id = ? AND owner_id = ?",
                PostgresDocumentRepository::readMetadata, documentId, ownerId).stream().findFirst();
    }

    @Override
    public List<DocumentMetadata> findMetadataByOwner(String ownerId) {
        return query(SELECT_METADATA + " WHERE owner_id = ?" + ORDER_NEWEST,
                PostgresDocumentRepository::readMetadata, ownerId);
    }

    @Override
    public Optional<DocumentMetadata> findLatestMetadataByReferenceAndOwner(
            DocumentType type, String referenceId, String ownerId) {
        String sql = SELECT_METADATA + " WHERE owner_id = ? AND document_type = ? AND reference_id = ?"
                + ORDER_NEWEST + " LIMIT 1";
        return query(sql, PostgresDocumentRepository::readMetadata, ownerId, type.name(), referenceId)
                .stream().findFirst();
    }

    @Override
    public Optional<Document> findByIdAndOwner(String documentId, String ownerId) {
        String sql = """
                SELECT document_type, reference_id, owner_id, customer_id, recipient_email, file_name, pdf_content
                FROM documents WHERE id = ? AND owner_id = ?
                """;
        return query(sql, row -> new Document(
                DocumentType.valueOf(row.getString("document_type")), row.getString("reference_id"),
                row.getString("owner_id"), row.getString("customer_id"), row.getString("recipient_email"),
                row.getString("file_name"), row.getBytes("pdf_content")), documentId, ownerId).stream().findFirst();
    }

    private <T> List<T> query(String sql, RowMapper<T> mapper, String... parameters) {
        try (var connection = dataSource.get().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < parameters.length; index++) {
                statement.setString(index + 1, parameters[index]);
            }
            try (ResultSet result = statement.executeQuery()) {
                List<T> rows = new ArrayList<>();
                while (result.next()) {
                    rows.add(mapper.read(result));
                }
                return rows;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not read documents", exception);
        }
    }

    private static DocumentMetadata readMetadata(ResultSet row) throws SQLException {
        return new DocumentMetadata(row.getString("id"), row.getString("reference_id"),
                DocumentType.valueOf(row.getString("document_type")), row.getString("file_name"));
    }

    @FunctionalInterface
    private interface RowMapper<T> {
        T read(ResultSet row) throws SQLException;
    }
}
