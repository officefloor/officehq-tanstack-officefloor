-- What a sheet held each time its today changed: the sheet's rows are copied into their own schema
-- (SNAP_<id>), kept with the today they stood at (today_was) and the today set next (today_now). A
-- screen shown as of a date reads the last copy taken as the sheet's today moved past that date.
CREATE SEQUENCE sheet_snapshot_seq;
CREATE TABLE sheet_snapshot (
    id        BIGINT       PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL,
    today_was DATE         NOT NULL,
    today_now DATE         NOT NULL
);
CREATE INDEX sheet_snapshot_sheet ON sheet_snapshot (sheet_id, id);
