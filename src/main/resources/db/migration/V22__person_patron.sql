-- The people a person names as their patrons: other people on the same sheet, each at most once.
CREATE TABLE person_patron (
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet(id),
    person_id BIGINT NOT NULL REFERENCES person(id),
    patron_id BIGINT NOT NULL REFERENCES person(id),
    PRIMARY KEY (person_id, patron_id)
);
CREATE INDEX person_patron_sheet ON person_patron(sheet_id);
