-- A DRAFT claim's lines, in the order added (by id): a label, a whole quantity of 1 or more and a unit amount; with
-- lines, the claim's amount is the quantities times the unit amounts added up.
CREATE TABLE claim_line (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200)   NOT NULL REFERENCES sheet (id),
    claim_id  BIGINT         NOT NULL REFERENCES claim (id),
    label     VARCHAR(200)   NOT NULL,
    qty       INT            NOT NULL,
    unit      DECIMAL(14, 2) NOT NULL
);
CREATE INDEX claim_line_sheet ON claim_line (sheet_id, claim_id, id);

-- Snapshotted with the rest of the screen (see V40, V41).
CREATE TABLE hist.claim_line AS SELECT t.*, CAST(NULL AS BIGINT) AS snapshot_id FROM claim_line t WHERE FALSE;
CREATE INDEX hist_claim_line_snapshot ON hist.claim_line (snapshot_id);
CREATE TABLE asof.claim_line AS SELECT * FROM claim_line WHERE FALSE;
