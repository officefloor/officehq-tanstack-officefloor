-- A person's patrons: other people on the same sheet they have named, each at most once.
CREATE TABLE patron (
    sheet_id  VARCHAR(200) NOT NULL REFERENCES sheet (id),
    person_id BIGINT       NOT NULL REFERENCES person (id),
    patron_id BIGINT       NOT NULL REFERENCES person (id),
    PRIMARY KEY (person_id, patron_id)
);
CREATE INDEX patron_sheet ON patron (sheet_id);
