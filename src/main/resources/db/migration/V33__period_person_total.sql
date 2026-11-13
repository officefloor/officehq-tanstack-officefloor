-- Each person's due-total for a closed period, fixed when it was closed: the dues of their counting claims dated
-- in it. A person with no row here (added after the close) shows 0.00 for it. Cleared when the period reopens.
CREATE TABLE period_person_total (
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet (id),
    period_name VARCHAR(100) NOT NULL,
    person_id BIGINT NOT NULL,
    total DECIMAL(19, 2) NOT NULL,
    PRIMARY KEY (sheet_id, period_name, person_id)
);
