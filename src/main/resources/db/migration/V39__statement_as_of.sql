-- A statement may be taken as of a date on or before the sheet's today: as_of is that date (null when it was taken
-- now), shown as the statement's date in place of taken_on.
ALTER TABLE statement ADD COLUMN as_of DATE;
-- The sheet as it stood when its today moved from from_today to to_today: every change made while the sheet's today
-- was from_today counts in it, and none made after. The state is the people, claims, rates and settings then (JSON).
CREATE TABLE sheet_state (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet      VARCHAR(200) NOT NULL,
    from_today DATE         NOT NULL,
    to_today   DATE         NOT NULL,
    state      CLOB         NOT NULL
);
CREATE INDEX sheet_state_sheet ON sheet_state (sheet, id);
