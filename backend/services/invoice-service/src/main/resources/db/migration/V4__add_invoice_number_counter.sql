CREATE TABLE invoice_number_counter (
    year_month varchar(6) PRIMARY KEY,
    last_number integer NOT NULL CHECK (last_number > 0)
);
