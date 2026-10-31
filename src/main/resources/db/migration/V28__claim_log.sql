-- A claim's status log: a line for every status it has had, in the order it had them — DRAFT when
-- it was added, then one per change. Each keeps who made it, the sheet's today then, and the claim's
-- due as it was worked out at that moment (null for n/a). Claims already kept have no lines.
CREATE TABLE claim_log (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200)  NOT NULL,
    claim_id BIGINT        NOT NULL REFERENCES claim (id),
    status   VARCHAR(20)   NOT NULL,
    by_user  VARCHAR(200)  NOT NULL,
    on_date  DATE          NOT NULL,
    due      DECIMAL(14,2)
);
CREATE INDEX claim_log_sheet ON claim_log (sheet_id, claim_id, id);
