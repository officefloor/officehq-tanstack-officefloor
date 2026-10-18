-- A claim's status log: one line per status it has had, written as the status is taken (DRAFT when the
-- claim is added): the status, the signed-in user who made the change, the sheet's today then and the
-- claim's due as computed at that moment (none when no rate applied). Listed oldest first (by id).
-- Existing claims have no log lines.
CREATE TABLE claim_log (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(100)   NOT NULL,
    claim_id  BIGINT         NOT NULL REFERENCES claim (id),
    status    VARCHAR(10)    NOT NULL,
    by_user   VARCHAR(100)   NOT NULL,
    log_date  DATE           NOT NULL,
    due       DECIMAL(14, 2)
);
CREATE INDEX claim_log_sheet ON claim_log (sheet_id, claim_id, id);
