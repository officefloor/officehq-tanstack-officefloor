-- The sheet's notes: a text, who added it and the sheet's today when it was added; listed newest first.
CREATE TABLE sheet_note (
    seq       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200)  NOT NULL,
    note_text VARCHAR(2000) NOT NULL,
    added_by  VARCHAR(200)  NOT NULL,
    added_on  DATE          NOT NULL
);
CREATE INDEX sheet_note_sheet ON sheet_note (sheet_id);
