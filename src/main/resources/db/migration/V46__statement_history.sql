-- Each statement taken of a person, in order (seq from 1), with its due-total ('n/a' when it had none) and date, so
-- the last one can be cancelled and the person shows the one before it again. A person's last statement before this
-- change is kept as their only known one.
CREATE TABLE person_statement (
    sheet_id VARCHAR(100) NOT NULL,
    person_id BIGINT NOT NULL,
    seq INT NOT NULL,
    stated_total VARCHAR(30),
    statement_date DATE,
    PRIMARY KEY (sheet_id, person_id, seq)
);
INSERT INTO person_statement (sheet_id, person_id, seq, stated_total, statement_date)
    SELECT sheet_id, id, statement_count, stated_total, statement_date FROM person WHERE statement_count > 0;
