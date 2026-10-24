-- A person's own terms from a date on: a claim of theirs dated on or after the from-date (and before the next one)
-- takes these terms (null: none, so their grade's or the sheet's apply). Before the first, person.terms applies.
CREATE TABLE person_terms (
    person_id BIGINT NOT NULL REFERENCES person (id),
    from_date DATE NOT NULL,
    terms INT
);
CREATE INDEX person_terms_person ON person_terms (person_id);
