-- Canonical schema for the locally verified, empty Squad database.
-- V1 remains unchanged. Refuse a conversion if any source records exist.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM offers)
        OR EXISTS (SELECT 1 FROM offer_positions)
        OR EXISTS (SELECT 1 FROM offer_status_history)
        OR EXISTS (SELECT 1 FROM offer_correction_suggestions)
        OR EXISTS (SELECT 1 FROM offer_customer_change_requests) THEN
        RAISE EXCEPTION 'V2 requires empty Squad tables. Preserve the database and perform a reviewed data migration instead.';
    END IF;
END $$;

DROP TABLE offer_positions;
DROP TABLE offer_status_history;
DROP TABLE offer_correction_suggestions;
DROP TABLE offer_customer_change_requests;
DROP TABLE offers;

    create sequence invoice_position_seq start with 1 increment by 50;

    create sequence invoice_seq start with 1 increment by 50;

    create sequence offer_position_seq start with 1 increment by 50;

    create sequence offer_seq start with 1 increment by 50;

    create sequence offer_status_history_seq start with 1 increment by 50;

    create table invoice (
        id bigint not null,
        created_at timestamp(6),
        updated_at timestamp(6),
        gesamt_preis numeric(15,2),
        kunde_email varchar(255),
        kunde_hausnummer varchar(255),
        kunde_nachname varchar(255),
        kunde_ort varchar(255),
        kunde_plz varchar(255),
        kunde_strasse varchar(255),
        kunde_vorname varchar(255),
        offer_business_key varchar(255) not null,
        rechnungsnummer varchar(255) not null unique,
        primary key (id)
    );

    create table invoice_position (
        id bigint not null,
        created_at timestamp(6),
        updated_at timestamp(6),
        bezeichnung varchar(255),
        einheit varchar(255),
        einzel_preis numeric(15,2),
        hersteller varchar(255),
        katalog_produkt_id varchar(255),
        menge numeric(38,4),
        positions_preis numeric(15,2),
        reihenfolge integer,
        type varchar(255) check ((type in ('MATERIAL','ANFAHRT','ARBEITSZEIT','OTHER'))),
        invoice_id bigint not null,
        primary key (id)
    );

    create table offer (
        id bigint not null,
        created_at timestamp(6),
        updated_at timestamp(6),
        annahme_token varchar(255) unique,
        business_key varchar(255) not null unique,
        customer_access_token_expires_at timestamp(6) with time zone,
        customer_access_token_hash varchar(255),
        customer_id varchar(255),
        gesamt_preis numeric(15,2),
        geschaetzte_arbeitsdauer_stunden numeric(19,4),
        handwerker_id varchar(255) not null,
        speech_snippet TEXT,
        status varchar(255) not null,
        offer_title varchar(255),
        primary key (id)
    );

    create table offer_customer_requests (
        offer_id bigint not null,
        request_text TEXT,
        request_order integer not null check ((request_order>=0)),
        primary key (offer_id, request_order)
    );

    create table offer_korrekturvorschlaege (
        offer_id bigint not null,
        vorschlag TEXT
    );

    create table offer_position (
        id bigint not null,
        created_at timestamp(6),
        updated_at timestamp(6),
        beschreibung varchar(255),
        bezeichnung varchar(255),
        einheit varchar(255),
        einzel_preis numeric(15,2),
        hersteller varchar(255),
        katalog_produkt_id varchar(255),
        menge numeric(38,4),
        positions_preis numeric(15,2),
        reihenfolge integer,
        type varchar(255) check ((type in ('MATERIAL','ANFAHRT','ARBEITSZEIT','OTHER'))),
        offer_id bigint not null,
        primary key (id)
    );

    create table offer_status_history (
        id bigint not null,
        created_at timestamp(6),
        updated_at timestamp(6),
        notiz varchar(255),
        status varchar(255) not null,
        zeitpunkt timestamp(6) not null,
        offer_id bigint not null,
        primary key (id)
    );

    alter table if exists invoice_position 
       add constraint FKdu7mlfhkxv9nm1pqc52x6wo2a 
       foreign key (invoice_id) 
       references invoice;

    alter table if exists offer_customer_requests 
       add constraint FK1l97d3gjmpir6rxen5msf9p8v 
       foreign key (offer_id) 
       references offer;

    alter table if exists offer_korrekturvorschlaege 
       add constraint FK3n6hux1rv2ip7gc2x4x11etye 
       foreign key (offer_id) 
       references offer;

    alter table if exists offer_position 
       add constraint FKc7wecg4uu561fh2rvrlt2y7ap 
       foreign key (offer_id) 
       references offer;

    alter table if exists offer_status_history 
       add constraint FK8iuuahpselkces17j1qanp5us 
       foreign key (offer_id) 
       references offer;

CREATE INDEX offer_owner_created_idx ON offer (handwerker_id, created_at DESC);
CREATE INDEX offer_position_order_idx ON offer_position (offer_id, reihenfolge);
CREATE INDEX offer_history_offer_time_idx ON offer_status_history (offer_id, zeitpunkt);