-- A claim's status log: one entry per status it has had, oldest first (by id), each with the user who
-- made the change, the sheet's today then, and the claim's due as computed at that moment (n/a when
-- no rate applied). Claims already kept have no entries.
CREATE TABLE claim_log (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id   VARCHAR(255) NOT NULL,
    claim_id   BIGINT       NOT NULL,
    status     VARCHAR(16)  NOT NULL,
    by_user    VARCHAR(255) NOT NULL,
    log_date   DATE         NOT NULL,
    due        VARCHAR(32)  NOT NULL
);
CREATE INDEX claim_log_sheet ON claim_log (sheet_id, claim_id, id);
