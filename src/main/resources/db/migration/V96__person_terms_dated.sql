-- A person's own terms may be dated: each row records their terms (days; null: none of their own, the grade's or the
-- sheet's apply) from a from-date. Their terms from the beginning of time stay in person.terms. A claim's due-by uses
-- the person's terms in force on its date: the row with the latest from-date on or before it, else person.terms.
CREATE TABLE person_terms (
    sheet_id   VARCHAR(100) NOT NULL,
    person_id  BIGINT       NOT NULL,
    from_date  DATE         NOT NULL,
    terms_days INT,
    PRIMARY KEY (sheet_id, person_id, from_date)
);
