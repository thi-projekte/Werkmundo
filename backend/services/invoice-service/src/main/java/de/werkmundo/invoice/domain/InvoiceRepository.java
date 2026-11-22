package de.werkmundo.invoice.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository {

    void insert(Invoice invoice);

    Optional<Invoice> findById(UUID id, String ownerId);

    List<Invoice> findByOwner(String ownerId);

    Optional<Invoice> findByIdempotencyKey(
            String ownerId,
            UUID idempotencyKey
    );

    Invoice insertOrGet(
            Invoice invoice,
            UUID idempotencyKey
    );
}