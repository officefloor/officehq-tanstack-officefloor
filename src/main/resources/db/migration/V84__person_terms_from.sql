-- A person's own terms recorded from a date: from that date on (until their next one's) a claim of theirs has these
-- terms (null: none, so their grade's or the sheet's). Their terms from the beginning of time stay in person.terms.
CREATE TABLE person_terms_from (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL,
    person_id BIGINT       NOT NULL,
    from_date DATE         NOT NULL,
    terms     INT
);
CREATE INDEX person_terms_from_person ON person_terms_from (sheet_id, person_id, from_date);
