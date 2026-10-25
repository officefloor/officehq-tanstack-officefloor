-- A claim may carry tags (null: none): trimmed, lower-cased, without repeats, sorted and joined by commas.
ALTER TABLE claim ADD COLUMN tags VARCHAR(1000);
ALTER TABLE hist.claim ADD COLUMN tags VARCHAR(1000);
ALTER TABLE asof.claim ADD COLUMN tags VARCHAR(1000);

-- Every tag ever used on a sheet's claims, kept when no claim carries it any more.
CREATE TABLE claim_tag (
    sheet_id VARCHAR(200)  NOT NULL REFERENCES sheet (id),
    tag      VARCHAR(1000) NOT NULL,
    PRIMARY KEY (sheet_id, tag)
);

-- Snapshotted with the rest of the screen (see V40, V41).
CREATE TABLE hist.claim_tag AS SELECT t.*, CAST(NULL AS BIGINT) AS snapshot_id FROM claim_tag t WHERE FALSE;
CREATE INDEX hist_claim_tag_snapshot ON hist.claim_tag (snapshot_id);
CREATE TABLE asof.claim_tag AS SELECT * FROM claim_tag WHERE FALSE;
