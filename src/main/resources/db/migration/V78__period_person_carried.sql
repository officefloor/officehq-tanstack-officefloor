-- Each person's carried figure fixed when a period was closed (or soft-closed); the rows go when it is reopened.
-- A person with no row for a closed period (added after it was closed) shows 0.00 for it.
CREATE TABLE period_person_carried (
    period_id BIGINT NOT NULL REFERENCES period (id),
    person_id BIGINT NOT NULL,
    carried DECIMAL(19, 2) NOT NULL,
    PRIMARY KEY (period_id, person_id)
);
