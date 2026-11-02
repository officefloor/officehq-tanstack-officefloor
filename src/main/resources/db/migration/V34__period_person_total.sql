-- Each person's due-total for a closed period, fixed when it was closed, for every person on the sheet
-- then (0.00 for one with nothing in it). Dropped when the period is reopened.
CREATE TABLE period_person_total (
    period_id BIGINT        NOT NULL REFERENCES period (id),
    sheet_id  VARCHAR(200)  NOT NULL REFERENCES sheet (id),
    person_id BIGINT        NOT NULL REFERENCES person (id),
    total     DECIMAL(19, 2) NOT NULL,
    PRIMARY KEY (period_id, person_id)
);
