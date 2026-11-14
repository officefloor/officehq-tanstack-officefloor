-- A claim's status log: one line per status it has had (DRAFT when it was added, then each change),
-- with who made the change, the sheet's today then, and the claim's due as computed at that moment
-- (none: n/a). Listed oldest first (by id). Claims added before the log have no lines.
CREATE TABLE claim_log (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200)   NOT NULL REFERENCES sheet (id),
    claim_id BIGINT         NOT NULL REFERENCES claim (id),
    status   VARCHAR(10)    NOT NULL,
    by_user  VARCHAR(200)   NOT NULL,
    on_date  DATE           NOT NULL,
    due      DECIMAL(19, 2)
);
CREATE INDEX claim_log_sheet ON claim_log (sheet_id, claim_id, id);
