-- A person's own terms are dated: person.terms are the ones from the beginning of time, and each row here records
-- terms (null: none, so the grade's or the sheet's) from its from-date; a claim uses the ones whose from-date is the
-- latest on or before its date. One row per person and from-date.
CREATE TABLE person_terms (
    person_id BIGINT NOT NULL,
    terms     INT,
    from_date DATE   NOT NULL,
    PRIMARY KEY (person_id, from_date)
);
