ALTER TABLE documents
    DROP CONSTRAINT documents_document_type_check;

ALTER TABLE documents
    ADD CONSTRAINT documents_document_type_check
        CHECK (document_type IN ('OFFER', 'INVOICE', 'ORDER_CONFIRMATION'));
