-- A closed period's due-total per person, fixed when it was closed: one row for every person on the sheet then.
-- A person added later has none (and shows 0.00); reopening the period drops its rows.
CREATE TABLE period_person_total (
    sheet  VARCHAR(200)   NOT NULL,
    period VARCHAR(200)   NOT NULL,
    person BIGINT         NOT NULL,
    total  DECIMAL(15, 2) NOT NULL,
    PRIMARY KEY (sheet, period, person)
);
