-- A claim's comments, in the order written (by id): each keeps the user who wrote it and the sheet's today then.
CREATE TABLE claim_comment (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet   VARCHAR(200)  NOT NULL,
    claim   BIGINT        NOT NULL REFERENCES claim (id),
    by_user VARCHAR(200)  NOT NULL,
    on_date DATE          NOT NULL,
    text    VARCHAR(4000) NOT NULL
);
CREATE INDEX claim_comment_sheet ON claim_comment (sheet, claim, id);
