-- Each person's due-total in a closed period, fixed when it was closed: one row per person on the sheet at that
-- moment (a person added later has none, and shows 0.00). The period's closed_total is these added up. Dropped
-- when the period is reopened.
CREATE TABLE period_person_total (
    sheet_id    VARCHAR(100)   NOT NULL REFERENCES sheet(id),
    period_name VARCHAR(200)   NOT NULL,
    person_id   BIGINT         NOT NULL REFERENCES person(id),
    total       DECIMAL(14, 2) NOT NULL,
    PRIMARY KEY (sheet_id, period_name, person_id)
);
