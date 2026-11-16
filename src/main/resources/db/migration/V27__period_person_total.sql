-- Closing a period also fixes each person's due-total for it, for every person on the sheet at that moment; the
-- period's closed_total is the sum of these. A person added later has no row here and shows 0.00 for the period.
-- Reopening the period drops its rows, so the figures are live again.
CREATE TABLE period_person_total (
    sheet_id  VARCHAR(200)   NOT NULL,
    period_id BIGINT         NOT NULL,
    person_id BIGINT         NOT NULL,
    total     DECIMAL(12, 2) NOT NULL,
    PRIMARY KEY (sheet_id, period_id, person_id)
);
