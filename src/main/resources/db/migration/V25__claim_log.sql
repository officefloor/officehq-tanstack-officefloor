-- A claim's status log: one line per status it has had, in the order it had them (by id). Each line keeps the
-- user who made the change, the sheet's today then, and the claim's due as it was computed at that moment
-- (two decimals, or n/a when its grade had no rate). A claim already kept has no lines for its past.
CREATE TABLE claim_log (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet   VARCHAR(200) NOT NULL,
    claim   BIGINT       NOT NULL REFERENCES claim (id),
    status  VARCHAR(20)  NOT NULL,
    by_user VARCHAR(200) NOT NULL,
    on_date DATE         NOT NULL,
    due     VARCHAR(20)  NOT NULL
);
CREATE INDEX claim_log_sheet ON claim_log (sheet, claim, id);
