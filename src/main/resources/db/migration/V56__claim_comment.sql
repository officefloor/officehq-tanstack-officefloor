-- A claim's comments, oldest first (by id): who wrote it, the sheet's today then (NULL when the sheet had none) and
-- its text (trimmed, never empty).
CREATE TABLE claim_comment (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(100)  NOT NULL REFERENCES sheet(id),
    claim_id BIGINT        NOT NULL REFERENCES claim(id),
    by_user  VARCHAR(100)  NOT NULL,
    on_date  DATE,
    text     VARCHAR(2000) NOT NULL
);
CREATE INDEX claim_comment_sheet ON claim_comment(sheet_id, claim_id, id);
