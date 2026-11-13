-- The sheet as it stood each time its today was changed (from_today -> to_today): its people, claims, rates, dated
-- settings and policy date as JSON, so a statement can be taken as of an earlier date. started is the sheet's today
-- when it was first loaded (nothing was on it before).
CREATE TABLE sheet_day (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet (id),
    from_today DATE NOT NULL,
    to_today DATE NOT NULL,
    state CLOB NOT NULL
);
CREATE INDEX sheet_day_sheet ON sheet_day (sheet_id, id);
ALTER TABLE sheet ADD COLUMN started DATE;
UPDATE sheet SET started = today;
