-- Each statement taken of a person, in the order taken: its due-total (NULL when it was n/a) and date. The person
-- keeps the last one's on their row; cancelling the last one removes it, and the row shows the one before it. A
-- person stated before keeps their last statement here (the earlier ones were not kept).
CREATE TABLE person_statement (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id       VARCHAR(100) NOT NULL REFERENCES sheet(id),
    person_id      BIGINT       NOT NULL REFERENCES person(id),
    due_total      DECIMAL(14, 2),
    statement_date DATE
);
CREATE INDEX person_statement_sheet ON person_statement(sheet_id, person_id);
INSERT INTO person_statement (sheet_id, person_id, due_total, statement_date)
    SELECT sheet_id, id, stated_total, statement_date FROM person WHERE statement_count > 0;
