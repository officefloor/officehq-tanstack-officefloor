-- A sheet's last rollover, kept so the steward who made it can undo it on the same sheet-today: who made it, the
-- sheet's today then, the old fiscal year, the periods it generated and the opening balances it set. Gone once undone.
CREATE TABLE last_rollover (
    sheet_id VARCHAR(200) PRIMARY KEY,
    made_by VARCHAR(255) NOT NULL,
    made_on DATE NOT NULL,
    fiscal_year VARCHAR(4) NOT NULL
);
CREATE TABLE last_rollover_period (
    sheet_id VARCHAR(200) NOT NULL REFERENCES last_rollover (sheet_id),
    period_id BIGINT NOT NULL
);
CREATE TABLE last_rollover_opening (
    sheet_id VARCHAR(200) NOT NULL REFERENCES last_rollover (sheet_id),
    period_id BIGINT NOT NULL,
    person_id BIGINT NOT NULL,
    amount DECIMAL(19, 2) NOT NULL
);
