-- Every proof added to a claim, with the sheet's today when it was added, so the sheet read as of a date shows the
-- proofs count as it stood then (proofs added before this was kept count from the beginning).
CREATE TABLE claim_proof_log (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    claim_id  BIGINT NOT NULL,
    made_on   DATE   NOT NULL
);
CREATE INDEX claim_proof_log_claim ON claim_proof_log (claim_id);
