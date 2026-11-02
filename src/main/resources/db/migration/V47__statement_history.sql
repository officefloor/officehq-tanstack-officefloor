-- Each statement taken of a person, numbered 1, 2, ... in the order taken, with its due-total (as shown,
-- so possibly 'n/a') and date. The person's stated_total and statement_date are those of the highest
-- numbered one; cancelling the last statement removes it, and the person then shows the one before it
-- (or nothing). A statement taken before this table only has its last one recorded.
CREATE TABLE person_statement (
    sheet_id       VARCHAR(200) NOT NULL REFERENCES sheet (id),
    person_id      BIGINT NOT NULL,
    seq            BIGINT NOT NULL,
    due_total      VARCHAR(20),
    statement_date DATE NOT NULL,
    PRIMARY KEY (sheet_id, person_id, seq)
);
INSERT INTO person_statement (sheet_id, person_id, seq, due_total, statement_date)
    SELECT sheet_id, id, statement_count, stated_total, statement_date FROM person
    WHERE statement_count > 0 AND statement_date IS NOT NULL;
