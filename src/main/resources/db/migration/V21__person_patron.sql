-- A person's patrons: other people on the same sheet they have named, each at most once and never
-- themselves. Listed in the order they were named (by id).
CREATE TABLE person_patron (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL,
    person_id BIGINT       NOT NULL REFERENCES person (id),
    patron_id BIGINT       NOT NULL REFERENCES person (id),
    CONSTRAINT person_patron_once UNIQUE (person_id, patron_id),
    CONSTRAINT person_patron_not_self CHECK (person_id <> patron_id)
);
CREATE INDEX person_patron_sheet ON person_patron (sheet_id, id);
