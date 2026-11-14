-- The people a person names as their patrons on the sheet: each at most once, never themselves.
CREATE TABLE person_patron (
    sheet_id   VARCHAR(200) NOT NULL REFERENCES sheet (id),
    person_id  BIGINT       NOT NULL REFERENCES person (id),
    patron_id  BIGINT       NOT NULL REFERENCES person (id),
    added_by   VARCHAR(200) NOT NULL,
    added_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (person_id, patron_id),
    CHECK (person_id <> patron_id)
);
