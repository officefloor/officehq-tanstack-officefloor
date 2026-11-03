-- A claim's comments, oldest first: the text (trimmed, never empty), who wrote it and the sheet's today then.
CREATE TABLE claim_comment (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200)  NOT NULL REFERENCES sheet (id),
    claim_id BIGINT        NOT NULL,
    by_user  VARCHAR(200)  NOT NULL,
    on_date  DATE          NOT NULL,
    text     VARCHAR(2000) NOT NULL
);
CREATE INDEX claim_comment_sheet ON claim_comment (sheet_id, claim_id, id);
