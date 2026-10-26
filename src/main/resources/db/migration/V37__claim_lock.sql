-- A claim whose editor a user has open: locked for every other user until saved or cancelled, or the sheet's today changes.
CREATE TABLE claim_lock (
    claim_id  BIGINT       PRIMARY KEY REFERENCES claim (id),
    sheet_id  VARCHAR(255) NOT NULL,
    locked_by VARCHAR(255) NOT NULL
);
CREATE INDEX claim_lock_sheet ON claim_lock (sheet_id);
