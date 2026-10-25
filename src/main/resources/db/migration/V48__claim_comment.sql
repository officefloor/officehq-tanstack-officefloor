-- A claim's comments, in the order written (by id): who wrote it, the sheet's today then, and its trimmed text.
CREATE TABLE claim_comment (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id   VARCHAR(200)  NOT NULL REFERENCES sheet (id),
    claim_id   BIGINT        NOT NULL REFERENCES claim (id),
    by_user    VARCHAR(200)  NOT NULL,
    written_on DATE          NOT NULL,
    text       VARCHAR(4000) NOT NULL
);
CREATE INDEX claim_comment_sheet ON claim_comment (sheet_id, claim_id, id);

-- Snapshotted with the rest of the screen (see V40, V41).
CREATE TABLE hist.claim_comment AS SELECT t.*, CAST(NULL AS BIGINT) AS snapshot_id FROM claim_comment t WHERE FALSE;
CREATE INDEX hist_claim_comment_snapshot ON hist.claim_comment (snapshot_id);
CREATE TABLE asof.claim_comment AS SELECT * FROM claim_comment WHERE FALSE;
