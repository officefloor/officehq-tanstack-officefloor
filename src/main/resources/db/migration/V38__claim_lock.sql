-- A claim's editor lock: the signed-in user whose open editor holds the claim. Others cannot open its
-- editor or save it until the holder saves or cancels, or the sheet's today changes (every lock is dropped).
CREATE TABLE claim_lock (
    claim_id  BIGINT       PRIMARY KEY REFERENCES claim (id),
    sheet_id  VARCHAR(200) NOT NULL REFERENCES sheet (id),
    locked_by VARCHAR(200) NOT NULL
);
CREATE INDEX claim_lock_sheet ON claim_lock (sheet_id);
