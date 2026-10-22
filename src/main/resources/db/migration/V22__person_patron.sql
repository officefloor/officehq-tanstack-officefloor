-- A person's patrons: other people on the same sheet, each named at most once. A person is never their own patron.
CREATE TABLE person_patron (
    sheet_id  VARCHAR(100) NOT NULL REFERENCES sheet(id),
    person_id BIGINT       NOT NULL REFERENCES person(id),
    patron_id BIGINT       NOT NULL REFERENCES person(id),
    PRIMARY KEY (person_id, patron_id),
    CHECK (person_id <> patron_id)
);
CREATE INDEX person_patron_sheet ON person_patron(sheet_id);
