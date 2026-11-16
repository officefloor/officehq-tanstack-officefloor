-- A claim's status log: one line per status it has had, in the order they happened (by id). Each line keeps who made
-- the change, the sheet's today when it was made, and the claim's due as shown at that moment (two decimals, or n/a).
-- Claims stored before the log existed have no lines.
CREATE TABLE claim_log (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL,
    claim_id  BIGINT       NOT NULL REFERENCES claim (id),
    status    VARCHAR(20)  NOT NULL,
    by_user   VARCHAR(200) NOT NULL,
    log_date  DATE         NOT NULL,
    due       VARCHAR(40)  NOT NULL
);
CREATE INDEX claim_log_sheet ON claim_log (sheet_id, claim_id, id);
