-- Every terms a person was given, oldest first, with the sheet's today when they were given (null
-- terms: none): a person's terms as of a date are the last given on or before it. What was already
-- stored counts from the beginning of time.
CREATE TABLE person_terms_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet(id),
    person_id BIGINT NOT NULL REFERENCES person(id),
    terms INT,
    set_on DATE NOT NULL
);
CREATE INDEX person_terms_history_sheet ON person_terms_history(sheet_id, person_id, id);
INSERT INTO person_terms_history (sheet_id, person_id, terms, set_on)
    SELECT sheet_id, id, terms, DATE '0001-01-01' FROM person;
