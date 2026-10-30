-- Each statement taken for a person, in the order taken: its due-total (null when n/a) and date. The
-- person's last statement can be cancelled, and the row then shows the one before it (or nothing).
-- A person's last statement taken before this history was kept is carried over; any before it are not
-- known, so cancelling it shows nothing.
CREATE TABLE person_statement (
    seq            BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id       VARCHAR(200) NOT NULL,
    person_id      BIGINT       NOT NULL,
    stated_total   DECIMAL(14,2),
    statement_date DATE
);
INSERT INTO person_statement (sheet_id, person_id, stated_total, statement_date)
    SELECT sheet_id, id, stated_total, statement_date FROM person WHERE statement_count > 0;
