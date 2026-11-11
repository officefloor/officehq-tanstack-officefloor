-- Each person's carried figure for a closed (or soft-closed) period, fixed when the period was
-- closed (one row per person on the sheet at that moment), as their due-total for it is. A person
-- with no row shows 0.00 for the closed period. Removed when the period is reopened, making the
-- figures live again.
CREATE TABLE period_person_carried (
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet(id),
    period_name VARCHAR(200) NOT NULL,
    person_id BIGINT NOT NULL,
    carried DECIMAL(19, 2) NOT NULL,
    PRIMARY KEY (sheet_id, period_name, person_id)
);
