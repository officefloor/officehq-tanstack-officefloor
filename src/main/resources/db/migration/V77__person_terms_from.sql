-- A person's own terms may be dated: each applies from its from-date (a later save for the same
-- from-date replaces it; null terms: none in force from it). Their terms with no from-date stay in
-- person.terms, in force from the beginning of time. A claim's due-by goes by the terms in force on
-- its date: the dated ones with the latest from-date on or before it, else those from the beginning.
CREATE TABLE person_terms (
    sheet_id  VARCHAR(200) NOT NULL,
    person_id BIGINT       NOT NULL,
    terms     INT,
    from_date DATE         NOT NULL,
    set_by    VARCHAR(200) NOT NULL,
    set_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX person_terms_from ON person_terms (person_id, from_date);
