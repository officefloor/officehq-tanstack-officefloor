-- Closing a period fixes each person's due-total for it too: one row per person on the sheet at that
-- moment. Reopening drops them; a person added while it is closed has no row (their figure is 0.00).
CREATE TABLE period_person_total (
    sheet_id    VARCHAR(255)   NOT NULL,
    period_name VARCHAR(255)   NOT NULL,
    person_id   BIGINT         NOT NULL,
    total       DECIMAL(15, 2) NOT NULL,
    PRIMARY KEY (sheet_id, period_name, person_id)
);
