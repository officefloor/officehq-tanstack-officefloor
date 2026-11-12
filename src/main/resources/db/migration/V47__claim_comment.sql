-- A claim's comments, oldest first (by id): each with the user who wrote it, the sheet's today when it was
-- written, and its text (trimmed, never empty). The as-of view shows those written on or before its date.
CREATE TABLE claim_comment (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id     VARCHAR(255)  NOT NULL,
    claim_id     BIGINT        NOT NULL,
    by_user      VARCHAR(255)  NOT NULL,
    written_on   DATE          NOT NULL,
    text         VARCHAR(4000) NOT NULL
);
CREATE INDEX claim_comment_sheet ON claim_comment (sheet_id, claim_id, id);
