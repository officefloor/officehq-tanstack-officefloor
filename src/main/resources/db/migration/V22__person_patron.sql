-- A person's patrons: other people on the same sheet they have named, each once. Their backing is the
-- patrons' due-totals added up.
CREATE TABLE person_patron (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet (id),
    person_id BIGINT NOT NULL REFERENCES person (id),
    patron_id BIGINT NOT NULL REFERENCES person (id),
    CONSTRAINT person_patron_unique UNIQUE (person_id, patron_id)
);
CREATE INDEX person_patron_sheet ON person_patron (sheet_id, id);
