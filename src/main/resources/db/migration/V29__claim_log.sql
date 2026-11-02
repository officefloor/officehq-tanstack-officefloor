-- A claim's status log: one row per status it has had (its DRAFT row written when it is added), who moved
-- it there, the sheet's today then, and its due as computed at that moment (null when it had none).
CREATE TABLE claim_log (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet (id),
    claim_id BIGINT       NOT NULL,
    status   VARCHAR(20)  NOT NULL,
    by_user  VARCHAR(200) NOT NULL,
    on_date  DATE         NOT NULL,
    due      DECIMAL(19, 2)
);
CREATE INDEX claim_log_sheet ON claim_log (sheet_id, claim_id, id);
