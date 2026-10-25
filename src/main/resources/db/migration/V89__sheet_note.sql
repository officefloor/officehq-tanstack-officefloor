-- A sheet's notes: a text, who added it, and the sheet's today when it was added (an as-of view shows the notes
-- added while the sheet's today was on or before its date).
CREATE TABLE sheet_note (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet (id),
    note_text VARCHAR(4000) NOT NULL,
    by_user VARCHAR(200) NOT NULL,
    on_date DATE NOT NULL
);
CREATE INDEX sheet_note_sheet ON sheet_note (sheet_id);
