-- A claim's status log: one row per status it has had (DRAFT when it was added, then each change), oldest
-- first by id, with the user who made the change, the sheet's today then, and the claim's due as it was
-- computed at that moment ('n/a' when it had none).
CREATE TABLE claim_log (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL,
    claim_id  BIGINT       NOT NULL,
    status    VARCHAR(20)  NOT NULL,
    by_user   VARCHAR(200) NOT NULL,
    log_date  DATE         NOT NULL,
    due       VARCHAR(40)  NOT NULL
);
CREATE INDEX claim_log_sheet ON claim_log (sheet_id, claim_id, id);
