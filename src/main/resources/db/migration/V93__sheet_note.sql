-- The sheet's notes: each its text (trimmed, not blank), who added it and the sheet's today when they did, listed
-- newest first.
CREATE TABLE sheet_note (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200)  NOT NULL,
    note_text VARCHAR(2000) NOT NULL,
    added_by  VARCHAR(200)  NOT NULL,
    added_on  DATE          NOT NULL
);
