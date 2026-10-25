-- Closing a period also fixes each person's due-total for it, for every person on the sheet at that moment
-- (a person added later has none: 0.00). The period's closed_total is the sum of these. Reopening removes them.
CREATE TABLE period_person_total (
    period_id BIGINT         NOT NULL REFERENCES period (id),
    person_id BIGINT         NOT NULL,
    total     DECIMAL(14, 2) NOT NULL,
    PRIMARY KEY (period_id, person_id)
);
