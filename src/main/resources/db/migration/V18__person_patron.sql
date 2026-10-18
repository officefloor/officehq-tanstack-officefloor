-- A person may name other people on the same sheet as their patrons: not themselves, and each only once.
CREATE TABLE person_patron (
    person_id BIGINT NOT NULL,
    patron_id BIGINT NOT NULL,
    PRIMARY KEY (person_id, patron_id),
    FOREIGN KEY (person_id) REFERENCES person (id),
    FOREIGN KEY (patron_id) REFERENCES person (id)
);
