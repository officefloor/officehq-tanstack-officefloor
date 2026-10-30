-- A claim's comments: one row per comment, oldest first by id, with the user who wrote it and the sheet's today then.
CREATE TABLE claim_comment (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id     VARCHAR(200)  NOT NULL,
    claim_id     BIGINT        NOT NULL,
    by_user      VARCHAR(200)  NOT NULL,
    comment_date DATE          NOT NULL,
    text         VARCHAR(4000) NOT NULL
);
CREATE INDEX claim_comment_sheet ON claim_comment (sheet_id, claim_id, id);
