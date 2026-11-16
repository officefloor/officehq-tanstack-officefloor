-- Every comment written on a claim: who wrote it, the sheet's today when it was written and its text (trimmed, not
-- blank), kept in the order written.
CREATE TABLE claim_comment (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id   VARCHAR(200)  NOT NULL,
    claim_id   BIGINT        NOT NULL REFERENCES claim (id),
    written_by VARCHAR(200)  NOT NULL,
    written_on DATE          NOT NULL,
    text       VARCHAR(2000) NOT NULL
);
CREATE INDEX claim_comment_claim ON claim_comment (sheet_id, claim_id, id);
