ALTER TABLE invoices
    ADD COLUMN idempotency_key uuid;

ALTER TABLE invoices
    ADD CONSTRAINT invoices_owner_idempotency_unique
    UNIQUE (owner_id, idempotency_key);