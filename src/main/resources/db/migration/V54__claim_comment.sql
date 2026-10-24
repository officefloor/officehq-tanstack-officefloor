-- A claim's comments: one row per comment, oldest first (by id) — its trimmed text, who wrote it and the sheet's today
-- when it was written.
CREATE TABLE claim_comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(255) NOT NULL,
    claim_id BIGINT NOT NULL,
    by_user VARCHAR(255) NOT NULL,
    on_date DATE NOT NULL,
    text VARCHAR(2000) NOT NULL
);
CREATE INDEX claim_comment_sheet ON claim_comment (sheet_id, claim_id);
