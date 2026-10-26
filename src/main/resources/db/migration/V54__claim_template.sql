-- The sheet's claim templates: a name (stored trimmed, at most once on its sheet: saving a name again replaces that
-- template), an amount the claim form would accept and a note. A claim made from one copies its amount and note then.
CREATE TABLE claim_template (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200)   NOT NULL REFERENCES sheet (id),
    name     VARCHAR(200)   NOT NULL,
    amount   DECIMAL(14, 2) NOT NULL,
    note     VARCHAR(1000)  NOT NULL
);
CREATE UNIQUE INDEX claim_template_sheet_name ON claim_template (sheet_id, name);

-- Snapshotted with the rest of the screen (see V40, V41).
CREATE TABLE hist.claim_template AS SELECT t.*, CAST(NULL AS BIGINT) AS snapshot_id FROM claim_template t WHERE FALSE;
CREATE INDEX hist_claim_template_snapshot ON hist.claim_template (snapshot_id);
CREATE TABLE asof.claim_template AS SELECT * FROM claim_template WHERE FALSE;
