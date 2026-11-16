-- Every deferral of a claim, with the date it was deferred until and the sheet's today when it was made, so the sheet
-- read as of a date shows the deferral as it stood then (a claim deferred before this was kept keeps its deferral).
CREATE TABLE claim_defer_log (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    claim_id   BIGINT NOT NULL,
    until_date DATE   NOT NULL,
    made_on    DATE   NOT NULL
);
CREATE INDEX claim_defer_log_claim ON claim_defer_log (claim_id, id);
