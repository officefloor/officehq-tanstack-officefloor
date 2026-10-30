-- Each person's due-total for a closed period, fixed when it was closed (dropped when it is reopened).
-- A person with no row for a closed period (added after it was closed) shows 0.00 for it.
CREATE TABLE period_person_total (
    sheet_id    VARCHAR(200)  NOT NULL,
    period_name VARCHAR(200)  NOT NULL,
    person_id   BIGINT        NOT NULL,
    total       DECIMAL(14,2) NOT NULL,
    PRIMARY KEY (sheet_id, period_name, person_id)
);
