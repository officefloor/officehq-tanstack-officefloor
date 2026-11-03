-- A person's own terms are dated: each terms given with a from-date is in force from it until the next one's.
-- person.terms keeps the ones recorded from the beginning of time (in force before the first from-date).
-- A person has at most one per from-date (giving one again replaces it); null terms clear them from that date
-- (the grade's or sheet's terms then apply).
CREATE TABLE person_terms (
    sheet_id  VARCHAR(200) NOT NULL REFERENCES sheet (id),
    person_id BIGINT       NOT NULL REFERENCES person (id),
    terms     INT,
    from_date DATE         NOT NULL
);
CREATE INDEX person_terms_person ON person_terms (person_id, from_date);
