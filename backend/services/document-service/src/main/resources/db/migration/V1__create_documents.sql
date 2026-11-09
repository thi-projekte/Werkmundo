HCREATE TABLE documents (
    id text PRIMARY KEY,
    owner_id text NOT NULL,
    document_type text NOT NULL CHECK (document_type IN ('OFFER', 'INVOICE')),
    reference_id text NOT NULL,
    customer_id text,
    recipient_email text,
    file_name text NOT NULL,
    pdf_content bytea NOT NULL CHECK (octet_length(pdf_content) > 0),
    created_at timestamptz NOT NULL DEFAULT clock_timestamp()
);

CREATE INDEX documents_owner_created_idx
    ON documents (owner_id, created_at DESC, id DESC);

CREATE INDEX documents_owner_reference_created_idx
    ON documents (owner_id, document_type, reference_id, created_at DESC, id DESC);
