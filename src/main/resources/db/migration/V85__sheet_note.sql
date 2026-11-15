-- The sheet's notes: a text, who added it and the sheet's today when it was added.
CREATE TABLE sheet_note (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id    VARCHAR(200)  NOT NULL REFERENCES sheet (id),
    text        VARCHAR(2000) NOT NULL,
    added_by    VARCHAR(200)  NOT NULL,
    added_on    DATE          NOT NULL,
    added_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
