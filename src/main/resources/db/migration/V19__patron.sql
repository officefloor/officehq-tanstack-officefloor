-- The patrons a person has named on the sheet: other people on it, each at most once.
CREATE TABLE patron (
    sheet     VARCHAR(200) NOT NULL,
    person_id BIGINT       NOT NULL,
    patron_id BIGINT       NOT NULL,
    PRIMARY KEY (sheet, person_id, patron_id)
);
