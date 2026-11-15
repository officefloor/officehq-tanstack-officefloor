-- A claim's comments: the text, the user who wrote it and the sheet's today when it was written.
-- Listed oldest first (by id).
CREATE TABLE claim_comment (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200)  NOT NULL REFERENCES sheet (id),
    claim_id BIGINT        NOT NULL REFERENCES claim (id),
    by_user  VARCHAR(200)  NOT NULL,
    on_date  DATE          NOT NULL,
    text     VARCHAR(2000) NOT NULL
);
CREATE INDEX claim_comment_sheet ON claim_comment (sheet_id, claim_id, id);
