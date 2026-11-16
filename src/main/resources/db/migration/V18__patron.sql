-- The people a person has named as their patrons: other people on the same sheet, each at most once.
CREATE TABLE patron (
    sheet_id  VARCHAR(200) NOT NULL,
    person_id BIGINT       NOT NULL REFERENCES person (id),
    patron_id BIGINT       NOT NULL REFERENCES person (id),
    PRIMARY KEY (sheet_id, person_id, patron_id)
);
