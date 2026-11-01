-- The sheet as it stood when its today moved from from_today to to_today: every change made while the sheet's today
-- was from_today counts in it, and none made after. Unlike sheet_state it keeps every row the sheet holds: each table
-- kept per sheet has a shadow table <table>__ASOF (made on boot, from the table's own columns) whose snapshot_id
-- names the row here, so the whole screen can be shown as it stood on a date.
CREATE TABLE sheet_snapshot (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet      VARCHAR(200) NOT NULL,
    from_today DATE         NOT NULL,
    to_today   DATE         NOT NULL
);
CREATE INDEX sheet_snapshot_sheet ON sheet_snapshot (sheet, id);
