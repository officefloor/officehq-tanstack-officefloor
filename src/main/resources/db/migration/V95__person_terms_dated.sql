-- A person's own terms are dated: person.terms are those in force from the beginning of time; each
-- row here is terms in force from its date (null terms: none, so their grade's or the sheet's), until
-- a later one. At most one row per person and from-date, kept by PersonRepository.save.
CREATE TABLE person_terms (
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet(id),
    person_id BIGINT NOT NULL REFERENCES person(id),
    from_date DATE NOT NULL,
    terms INT,
    PRIMARY KEY (person_id, from_date)
);
CREATE INDEX person_terms_sheet ON person_terms(sheet_id, person_id);
-- The history keeps the from-date each terms were given from (null: from the beginning of time).
ALTER TABLE person_terms_history ADD COLUMN from_date DATE;
