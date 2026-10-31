-- Every statement taken for a person, numbered from 1 in the order taken (`seq`), with its
-- due-total (null when n/a) and date: so cancelling the last one can show the one before it.
-- The person's statement_count, stated_total and statement_date stay the last one's.
CREATE TABLE person_statement (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id    VARCHAR(200) NOT NULL,
    person_id   BIGINT       NOT NULL REFERENCES person (id),
    seq         INT          NOT NULL,
    stated_total DECIMAL(14,2),
    statement_date DATE      NOT NULL
);
CREATE INDEX person_statement_person ON person_statement (person_id, seq);
-- A statement taken before this was kept is known only as the last one.
INSERT INTO person_statement (sheet_id, person_id, seq, stated_total, statement_date)
    SELECT sheet_id, id, statement_count, stated_total, statement_date FROM person
    WHERE statement_count > 0 AND statement_date IS NOT NULL;
