-- Opening balances pasted for a person and a period: each line's amount (0.00 or more, two decimals) adds to the
-- person's carried figure for the period while it is open, and is fixed into it when the period is closed.
CREATE TABLE opening_balance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet (id),
    period_name VARCHAR(100) NOT NULL,
    person_id BIGINT NOT NULL REFERENCES person (id),
    amount DECIMAL(15, 2) NOT NULL
);
CREATE INDEX opening_balance_sheet ON opening_balance (sheet_id, period_name);
