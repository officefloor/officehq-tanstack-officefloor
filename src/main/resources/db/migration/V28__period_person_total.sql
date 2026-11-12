-- Each person's due-total in a closed period, fixed when the period was closed (the figures of their
-- claims that counted in it, skipping n/a). The period's closed_total is the sum of these. Dropped on
-- reopening; a person with no row here shows 0.00 for the closed period.
CREATE TABLE period_person_total (
    sheet_id    VARCHAR(255)   NOT NULL,
    period_name VARCHAR(255)   NOT NULL,
    person_id   BIGINT         NOT NULL,
    total       DECIMAL(19, 2) NOT NULL,
    PRIMARY KEY (sheet_id, period_name, person_id)
);
