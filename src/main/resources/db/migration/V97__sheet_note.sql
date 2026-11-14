-- The sheet's notes, oldest first (id order): the text, the user who added it and the sheet's today then.
CREATE TABLE sheet_note (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet (id),
    by_user VARCHAR(200) NOT NULL,
    note_date DATE NOT NULL,
    text VARCHAR(4000) NOT NULL
);
CREATE INDEX sheet_note_sheet ON sheet_note (sheet_id, id);
