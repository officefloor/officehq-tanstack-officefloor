-- Each person's due-total for a closed period, fixed when the period was closed (the period's own
-- `closed_total` is their sum). A person with no row shows 0.00 for it. Reopening deletes the rows.
CREATE TABLE period_person_total (
    sheet_id    VARCHAR(200)  NOT NULL,
    period_name VARCHAR(200)  NOT NULL,
    person_id   BIGINT        NOT NULL,
    total       DECIMAL(14,2) NOT NULL,
    PRIMARY KEY (sheet_id, period_name, person_id)
);
