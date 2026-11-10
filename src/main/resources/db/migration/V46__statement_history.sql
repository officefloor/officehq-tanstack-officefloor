-- Each statement taken for a person, numbered from 1, with its due-total (n/a when it had none) and
-- date. The person row keeps the last one's; cancelling the last shows the one before it again.
CREATE TABLE person_statement (
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet(id),
    person_id BIGINT NOT NULL REFERENCES person(id),
    seq INT NOT NULL,
    stated_total VARCHAR(30),
    statement_date DATE,
    PRIMARY KEY (sheet_id, person_id, seq)
);
-- The last statement of each person is already known; earlier ones were not kept.
INSERT INTO person_statement (sheet_id, person_id, seq, stated_total, statement_date)
    SELECT sheet_id, id, statement_count, stated_total, statement_date FROM person WHERE statement_count > 0;
