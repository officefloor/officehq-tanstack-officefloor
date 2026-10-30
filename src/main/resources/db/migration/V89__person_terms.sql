-- A person's own terms can be dated: each row is the terms in force from its from-date until the
-- next one (a null terms: none then, so the grade's or the sheet's apply). The terms from the
-- beginning of time stay in person.terms. One row per (sheet, person, from-date).
CREATE TABLE person_terms (
    seq       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL,
    person_id BIGINT       NOT NULL,
    from_date DATE         NOT NULL,
    terms     INT
);
CREATE INDEX person_terms_person ON person_terms (sheet_id, person_id);
