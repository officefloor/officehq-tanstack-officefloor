-- A sheet's notes: the text (trimmed, not blank), who added it and the sheet's today when it was added.
-- Listed newest first (by id).
CREATE TABLE sheet_note (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200)  NOT NULL REFERENCES sheet (id),
    added_on  DATE          NOT NULL,
    added_by  VARCHAR(200)  NOT NULL,
    text      VARCHAR(2000) NOT NULL
);
