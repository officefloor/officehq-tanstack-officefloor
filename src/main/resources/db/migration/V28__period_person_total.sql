-- A closed period's figures per person: each person on the sheet when it was closed, with the due of
-- their claims that count dated in it, fixed at that moment. A person added later has none (0.00).
-- Removed when the period is reopened. The period's due-total is the sum of these.
CREATE TABLE period_person_total (
    period_id BIGINT        NOT NULL REFERENCES period (id),
    person_id BIGINT        NOT NULL REFERENCES person (id),
    total     DECIMAL(19, 2) NOT NULL,
    PRIMARY KEY (period_id, person_id)
);
