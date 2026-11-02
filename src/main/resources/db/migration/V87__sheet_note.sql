-- The sheet's notes: each with the sheet's today when it was added and who added it. Not kept in the sheet's as-of
-- history: a screen as of a date shows the notes added while the sheet's today was on or before it.
CREATE TABLE sheet_note (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet    VARCHAR(200)  NOT NULL,
    added_on DATE          NOT NULL,
    added_by VARCHAR(200)  NOT NULL,
    text     VARCHAR(2000) NOT NULL
);
CREATE INDEX sheet_note_sheet ON sheet_note (sheet);
