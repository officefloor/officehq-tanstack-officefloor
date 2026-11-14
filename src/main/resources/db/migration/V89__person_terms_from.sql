-- A person's own terms recorded from a from-date, beside person.terms (their terms from the beginning of time).
-- The terms in force on a date are those with the latest from-date on or before it; terms null: none of their own
-- from that date (their grade's or the sheet's apply).
CREATE TABLE person_terms (
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet (id),
    person_id BIGINT NOT NULL REFERENCES person (id),
    terms INT,
    from_date DATE NOT NULL
);
CREATE INDEX person_terms_sheet ON person_terms (sheet_id, person_id);
