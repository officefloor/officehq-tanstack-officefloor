-- A person's patrons: other people on the same sheet, each named at most once, never the person.
CREATE TABLE person_patron (
    sheet_id  VARCHAR(100) NOT NULL,
    person_id BIGINT       NOT NULL,
    patron_id BIGINT       NOT NULL,
    PRIMARY KEY (sheet_id, person_id, patron_id),
    CHECK (person_id <> patron_id)
);
