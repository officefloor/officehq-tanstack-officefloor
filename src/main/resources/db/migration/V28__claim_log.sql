-- A claim's status log: one entry per status it has had (its DRAFT one written when it was added), oldest first
-- (by id): who gave it the status, the sheet's today then (NULL when the sheet had none) and the claim's due as its
-- row showed it then (NULL when n/a).
CREATE TABLE claim_log (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(100)   NOT NULL REFERENCES sheet(id),
    claim_id BIGINT         NOT NULL REFERENCES claim(id),
    status   VARCHAR(20)    NOT NULL,
    by_user  VARCHAR(100)   NOT NULL,
    on_date  DATE,
    due      DECIMAL(14, 2)
);
CREATE INDEX claim_log_sheet ON claim_log(sheet_id, claim_id, id);
