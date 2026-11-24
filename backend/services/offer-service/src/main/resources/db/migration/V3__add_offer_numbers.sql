ALTER TABLE offer
    ADD COLUMN angebotsnummer varchar(32);

CREATE UNIQUE INDEX uq_offer_angebotsnummer
    ON offer (angebotsnummer);

CREATE TABLE offer_number_counter (
    year_month varchar(6) PRIMARY KEY,
    last_number integer NOT NULL CHECK (last_number > 0)
);
