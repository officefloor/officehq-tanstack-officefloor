-- A claim's status log: one line per status it has had, in the order it had them (by id) — who made the change, the
-- sheet's today then, and the claim's due as computed at that moment (null: n/a). Its DRAFT line is written when it is added.
CREATE TABLE claim_log (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200)   NOT NULL REFERENCES sheet (id),
    claim_id  BIGINT         NOT NULL REFERENCES claim (id),
    status    VARCHAR(10)    NOT NULL,
    by_user   VARCHAR(200)   NOT NULL,
    logged_on DATE           NOT NULL,
    due       DECIMAL(14, 2)
);
CREATE INDEX claim_log_sheet ON claim_log (sheet_id, claim_id, id);
