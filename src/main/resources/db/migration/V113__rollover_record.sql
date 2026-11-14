-- Each year-end rollover made on a sheet: the old fiscal year (FY<yyyy>), the user who made it and the sheet's today
-- it was made on; undone once taken back. The periods it generated are listed in rollover_period, and the opening
-- balances it set carry its id (rollover_id), so an undo removes exactly those.
CREATE TABLE rollover (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet (id),
    fy VARCHAR(30) NOT NULL,
    made_by VARCHAR(255) NOT NULL,
    made_on DATE NOT NULL,
    undone BOOLEAN DEFAULT FALSE NOT NULL
);
CREATE INDEX rollover_sheet ON rollover (sheet_id);
CREATE TABLE rollover_period (
    rollover_id BIGINT NOT NULL REFERENCES rollover (id),
    period_name VARCHAR(100) NOT NULL
);
ALTER TABLE opening_balance ADD COLUMN rollover_id BIGINT;
