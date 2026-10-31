-- Each year end rollover on a sheet: who made it, on which of the sheet's todays, of which fiscal year
-- (FY<yyyy>), and the names of the periods it generated (joined by ","). undone once it has been undone.
-- The last rollover of a sheet is the one with the highest id.
CREATE TABLE rollover (
    id          BIGINT        AUTO_INCREMENT PRIMARY KEY,
    sheet_id    VARCHAR(200)  NOT NULL,
    made_by     VARCHAR(200)  NOT NULL,
    made_on     DATE          NOT NULL,
    fiscal_year VARCHAR(20)   NOT NULL,
    periods     VARCHAR(4000) NOT NULL DEFAULT '',
    undone      BOOLEAN       NOT NULL DEFAULT FALSE
);
CREATE INDEX rollover_sheet ON rollover (sheet_id, id);
-- The rollover that set an opening row; NULL for one pasted or imported.
ALTER TABLE period_opening ADD COLUMN rollover_id BIGINT;
