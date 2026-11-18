CREATE TABLE invoices (
    id uuid PRIMARY KEY,
    owner_id text NOT NULL,
    offer_business_key text NOT NULL,
    rechnungsnummer text,

    kunde_vorname text,
    kunde_nachname text,
    kunde_email text,
    kunde_strasse text,
    kunde_hausnummer text,
    kunde_plz text,
    kunde_ort text,

    gesamt_preis numeric(15, 2) NOT NULL
        CHECK (gesamt_preis >= 0),

    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,

    CONSTRAINT invoices_owner_number_unique
        UNIQUE (owner_id, rechnungsnummer)
);

CREATE INDEX invoices_owner_created_idx
    ON invoices (owner_id, created_at DESC, id DESC);

CREATE INDEX invoices_owner_offer_idx
    ON invoices (owner_id, offer_business_key);

CREATE TABLE invoice_positions (
    invoice_id uuid NOT NULL
        REFERENCES invoices (id) ON DELETE CASCADE,

    reihenfolge integer NOT NULL CHECK (reihenfolge >= 0),
    hersteller text,
    bezeichnung text NOT NULL,
    menge numeric(38, 4) NOT NULL CHECK (menge > 0),
    einheit text NOT NULL,
    katalog_produkt_id text,
    einzel_preis numeric(15, 2) NOT NULL CHECK (einzel_preis >= 0),
    positions_preis numeric(15, 2) NOT NULL CHECK (positions_preis >= 0),
    position_type text NOT NULL
        CHECK (position_type IN ('MATERIAL', 'ANFAHRT', 'ARBEITSZEIT', 'OTHER')),

    PRIMARY KEY (invoice_id, reihenfolge)
);