-- A person's patrons: other people on the same sheet they named, each at most once.
CREATE TABLE patron (
    sheet_id  VARCHAR(255) NOT NULL,
    person_id BIGINT       NOT NULL,
    patron_id BIGINT       NOT NULL,
    PRIMARY KEY (sheet_id, person_id, patron_id)
);
