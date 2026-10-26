-- The sheet's notes: a text (stored trimmed, never blank), the signed-in user who added it and the sheet's today when
-- it was added. Listed newest first.
CREATE TABLE sheet_note (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200)  NOT NULL REFERENCES sheet (id),
    added_on DATE          NOT NULL,
    added_by VARCHAR(200)  NOT NULL,
    text     VARCHAR(2000) NOT NULL
);
CREATE INDEX sheet_note_sheet ON sheet_note (sheet_id);

-- Snapshotted with the rest of the screen (see V40, V41).
CREATE TABLE hist.sheet_note AS SELECT n.*, CAST(NULL AS BIGINT) AS snapshot_id FROM sheet_note n WHERE FALSE;
CREATE INDEX hist_sheet_note_snapshot ON hist.sheet_note (snapshot_id);
CREATE TABLE asof.sheet_note AS SELECT * FROM sheet_note WHERE FALSE;
