-- Each person's opening balance for a period: the amounts pasted for it, added up (two decimals, 0.00 or more).
-- Added to their carried figure for the period while it is open, and fixed into it when it is closed.
CREATE TABLE period_opening (
    period_id BIGINT         NOT NULL REFERENCES period (id),
    sheet_id  VARCHAR(200)   NOT NULL REFERENCES sheet (id),
    person_id BIGINT         NOT NULL REFERENCES person (id),
    amount    DECIMAL(19, 2) NOT NULL,
    PRIMARY KEY (period_id, person_id)
);
