-- A claim's status log, oldest first (by id): each status it has had, who gave it, the sheet's today then,
-- and its due as shown at that moment ("n/a" when it had none). Claims made before the log have none.
CREATE TABLE claim_log (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(255) NOT NULL,
    claim_id  BIGINT       NOT NULL REFERENCES claim (id),
    status    VARCHAR(16)  NOT NULL,
    by_user   VARCHAR(255) NOT NULL,
    logged_on DATE         NOT NULL,
    due       VARCHAR(32)  NOT NULL
);
CREATE INDEX claim_log_sheet ON claim_log (sheet_id, claim_id, id);
