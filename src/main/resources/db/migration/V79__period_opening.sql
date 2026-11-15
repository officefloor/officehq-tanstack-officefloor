-- A person's opening balance for a period: pasted amounts added up. It is added to their carried
-- figure for the period (and fixed with it when the period is closed or soft-closed).
CREATE TABLE period_opening (
    period_id BIGINT         NOT NULL REFERENCES period (id),
    person_id BIGINT         NOT NULL REFERENCES person (id),
    amount    DECIMAL(19, 2) NOT NULL,
    PRIMARY KEY (period_id, person_id)
);
