-- Each statement taken of a person, in the order taken: its due-total (text: two decimals, or n/a)
-- and date. Kept so a steward can cancel the last one and the person shows the one before it again.
-- A person stated before statements were kept keeps their last one (the earlier ones are not known).
CREATE TABLE person_statement (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id       VARCHAR(200) NOT NULL REFERENCES sheet (id),
    person_id      BIGINT       NOT NULL REFERENCES person (id),
    stated_total   VARCHAR(30),
    statement_date DATE         NOT NULL,
    taken_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX person_statement_person ON person_statement (sheet_id, person_id, id);
INSERT INTO person_statement (sheet_id, person_id, stated_total, statement_date)
    SELECT sheet_id, id, stated_total, statement_date FROM person WHERE statement_count > 0 AND statement_date IS NOT NULL;
