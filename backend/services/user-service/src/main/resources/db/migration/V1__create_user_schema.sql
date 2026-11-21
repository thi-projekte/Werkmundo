-- Werkmundo / user-service: Schema der user-db
--
-- Grundlage: Architekturvorschlag "Werkmundo User-Datenbank" (13.11.2026).
-- Passwörter werden NICHT gespeichert; Login und Registrierung laufen über Keycloak.
-- Kunden gehören nicht in diese Datenbank (Squad J, siehe documents/team-ownership.md).
--
-- Das SQL ist bewusst portabel gehalten (PostgreSQL im Betrieb, H2 im PostgreSQL-Modus in Tests):
-- keine DB-seitigen UUID-Defaults, keine partiellen Indizes, VARCHAR statt TEXT.
-- IDs werden in Java erzeugt.

-- ---------------------------------------------------------------------------
-- Betrieb (Mandant). Bei der Registrierung wird nur der Name gesetzt;
-- die übrigen Pflichtangaben werden im Onboarding ergänzt und dort geprüft.
-- ---------------------------------------------------------------------------
CREATE TABLE companies (
    id                     UUID PRIMARY KEY,
    name                   VARCHAR(200)  NOT NULL,
    legal_form             VARCHAR(50),
    trade                  VARCHAR(100),
    street                 VARCHAR(200),
    house_number           VARCHAR(20),
    postal_code            VARCHAR(10),
    city                   VARCHAR(100),
    country                VARCHAR(2)    NOT NULL DEFAULT 'DE',
    email                  VARCHAR(254),
    phone                  VARCHAR(40),
    contact_person         VARCHAR(200),
    website                VARCHAR(300),
    logo_url               VARCHAR(500),
    trade_register_number  VARCHAR(50),
    register_court         VARCHAR(100),
    chamber_registration   VARCHAR(100),
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL
);

-- ---------------------------------------------------------------------------
-- Benutzerkonto, verbunden mit Keycloak über keycloak_id (Claim "sub").
-- ---------------------------------------------------------------------------
CREATE TABLE users (
    id                    UUID PRIMARY KEY,
    company_id            UUID          NOT NULL REFERENCES companies (id),
    keycloak_id           VARCHAR(64)   NOT NULL,
    email                 VARCHAR(254)  NOT NULL,
    first_name            VARCHAR(100)  NOT NULL,
    last_name             VARCHAR(100)  NOT NULL,
    phone                 VARCHAR(40),
    profile_picture_url   VARCHAR(500),
    role                  VARCHAR(20)   NOT NULL,
    status                VARCHAR(20)   NOT NULL,
    onboarding_completed  BOOLEAN       NOT NULL DEFAULT FALSE,
    last_login_at         TIMESTAMP WITH TIME ZONE,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_users_keycloak_id UNIQUE (keycloak_id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('OWNER', 'EMPLOYEE', 'ACCOUNTANT')),
    CONSTRAINT ck_users_status CHECK (status IN ('PENDING', 'ACTIVE', 'DISABLED', 'DELETED'))
);

CREATE INDEX idx_users_company_id ON users (company_id);

-- ---------------------------------------------------------------------------
-- Bankverbindung für Rechnungen. Genau ein Standardkonto pro Betrieb
-- stellt der Service sicher (H2 kennt keine partiellen Unique-Indizes).
-- ---------------------------------------------------------------------------
CREATE TABLE bank_accounts (
    id              UUID PRIMARY KEY,
    company_id      UUID          NOT NULL REFERENCES companies (id) ON DELETE CASCADE,
    account_holder  VARCHAR(200)  NOT NULL,
    iban            VARCHAR(34)   NOT NULL,
    bic             VARCHAR(11),
    bank_name       VARCHAR(200),
    is_default      BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_bank_accounts_company_iban UNIQUE (company_id, iban)
);

CREATE INDEX idx_bank_accounts_company_id ON bank_accounts (company_id);

-- ---------------------------------------------------------------------------
-- Steuer- und Rechnungseinstellungen (1:1 zum Betrieb).
-- Steuernummer oder USt-IdNr. muss vorhanden sein (§ 14 Abs. 4 UStG).
-- Die fortlaufende Rechnungsnummer zählt der Rechnungsservice, hier liegt nur das Präfix.
-- ---------------------------------------------------------------------------
CREATE TABLE tax_invoice_settings (
    company_id         UUID          PRIMARY KEY REFERENCES companies (id) ON DELETE CASCADE,
    vat_treatment      VARCHAR(20)   NOT NULL,
    default_vat_rate   NUMERIC(5, 2) NOT NULL,
    tax_number         VARCHAR(30),
    vat_id             VARCHAR(20),
    invoice_prefix     VARCHAR(20)   NOT NULL,
    offer_prefix       VARCHAR(20),
    payment_term_days  INTEGER       NOT NULL,
    invoice_email      VARCHAR(254),
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_tax_vat_treatment CHECK (vat_treatment IN ('REGULAR', 'SMALL_BUSINESS')),
    CONSTRAINT ck_tax_vat_rate CHECK (default_vat_rate >= 0 AND default_vat_rate <= 100),
    CONSTRAINT ck_tax_small_business_rate CHECK (vat_treatment <> 'SMALL_BUSINESS' OR default_vat_rate = 0),
    CONSTRAINT ck_tax_identifier CHECK (tax_number IS NOT NULL OR vat_id IS NOT NULL),
    CONSTRAINT ck_tax_payment_term CHECK (payment_term_days >= 0)
);

-- ---------------------------------------------------------------------------
-- Lohngruppen mit Stundensatz (netto, Euro) für die Kalkulation im offer-service.
-- ---------------------------------------------------------------------------
CREATE TABLE employee_groups (
    id           UUID PRIMARY KEY,
    company_id   UUID           NOT NULL REFERENCES companies (id) ON DELETE CASCADE,
    name         VARCHAR(100)   NOT NULL,
    hourly_rate  NUMERIC(10, 2) NOT NULL,
    is_default   BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_employee_groups_company_name UNIQUE (company_id, name),
    CONSTRAINT ck_employee_groups_rate CHECK (hourly_rate >= 0)
);

CREATE INDEX idx_employee_groups_company_id ON employee_groups (company_id);

-- ---------------------------------------------------------------------------
-- Anfahrtskosten (1:1 zum Betrieb).
-- ---------------------------------------------------------------------------
CREATE TABLE travel_cost_settings (
    company_id  UUID           PRIMARY KEY REFERENCES companies (id) ON DELETE CASCADE,
    model       VARCHAR(20)    NOT NULL,
    flat_rate   NUMERIC(10, 2),
    km_rate     NUMERIC(10, 2),
    free_km     INTEGER,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_travel_model CHECK (model IN ('FLAT', 'FLAT_PLUS_KM', 'KM_ONLY')),
    CONSTRAINT ck_travel_flat_rate CHECK (model = 'KM_ONLY' OR flat_rate IS NOT NULL),
    CONSTRAINT ck_travel_km_rate CHECK (model = 'FLAT' OR km_rate IS NOT NULL),
    CONSTRAINT ck_travel_non_negative CHECK (
        (flat_rate IS NULL OR flat_rate >= 0)
        AND (km_rate IS NULL OR km_rate >= 0)
        AND (free_km IS NULL OR free_km >= 0)
    )
);

-- ---------------------------------------------------------------------------
-- Standardtexte für Angebote und Rechnungen, ein Text je Typ und Betrieb.
-- ---------------------------------------------------------------------------
CREATE TABLE text_templates (
    id          UUID PRIMARY KEY,
    company_id  UUID            NOT NULL REFERENCES companies (id) ON DELETE CASCADE,
    type        VARCHAR(30)     NOT NULL,
    content     VARCHAR(20000)  NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_text_templates_company_type UNIQUE (company_id, type),
    CONSTRAINT ck_text_templates_type CHECK (
        type IN ('TERMS', 'PAYMENT_TERMS', 'OFFER_INTRO', 'OFFER_FOOTER', 'INVOICE_FOOTER')
    )
);

-- ---------------------------------------------------------------------------
-- KI-Einstellungen für den AI-service (1:1 zum Betrieb).
-- ---------------------------------------------------------------------------
CREATE TABLE ai_preferences (
    company_id     UUID         PRIMARY KEY REFERENCES companies (id) ON DELETE CASCADE,
    tone_of_voice  VARCHAR(20)  NOT NULL DEFAULT 'FORMAL',
    detail_level   VARCHAR(20)  NOT NULL DEFAULT 'NORMAL',
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_ai_tone CHECK (tone_of_voice IN ('FORMAL', 'FRIENDLY')),
    CONSTRAINT ck_ai_detail CHECK (detail_level IN ('SHORT', 'NORMAL', 'DETAILED'))
);

-- ---------------------------------------------------------------------------
-- Zustimmungen zu AGB und Datenschutz (DSGVO-Nachweis).
-- ---------------------------------------------------------------------------
CREATE TABLE consents (
    id           UUID PRIMARY KEY,
    user_id      UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type         VARCHAR(20)  NOT NULL,
    version      VARCHAR(20)  NOT NULL,
    accepted_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_consents_user_type_version UNIQUE (user_id, type, version),
    CONSTRAINT ck_consents_type CHECK (type IN ('TERMS', 'PRIVACY'))
);

CREATE INDEX idx_consents_user_id ON consents (user_id);

-- ---------------------------------------------------------------------------
-- Änderungsprotokoll. Keine sensiblen Werte in description (z. B. IBAN nur gekürzt).
-- user_id bleibt erhalten, auch wenn ein Konto später anonymisiert wird.
-- ---------------------------------------------------------------------------
CREATE TABLE audit_logs (
    id           UUID PRIMARY KEY,
    company_id   UUID          NOT NULL REFERENCES companies (id) ON DELETE CASCADE,
    user_id      UUID          REFERENCES users (id) ON DELETE SET NULL,
    action       VARCHAR(60)   NOT NULL,
    description  VARCHAR(2000),
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_audit_logs_company_created ON audit_logs (company_id, created_at);
