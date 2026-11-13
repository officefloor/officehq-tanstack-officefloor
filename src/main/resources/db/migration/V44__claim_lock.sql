-- A claim whose editor a user has open is locked for every other user until that user saves or cancels the editor,
-- or the sheet's today changes. At most one lock per claim.
CREATE TABLE claim_lock (
    sheet_id VARCHAR(255) NOT NULL,
    claim_id BIGINT NOT NULL,
    locked_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (sheet_id, claim_id)
);
