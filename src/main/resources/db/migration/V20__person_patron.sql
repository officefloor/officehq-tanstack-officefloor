-- A person's patrons: other people on the same sheet they have named, each at most once, in the
-- order they were named (seq).
CREATE TABLE person_patron (
    seq       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL,
    person_id BIGINT       NOT NULL,
    patron_id BIGINT       NOT NULL,
    UNIQUE (sheet_id, person_id, patron_id)
);
