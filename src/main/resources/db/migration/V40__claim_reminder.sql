-- Every reminder written for a claim: the sheet's today when it was written. A claim is reminded at most once per
-- sheet-today; its reminded date is the last of these (as of a date: the last on or before it).
CREATE TABLE claim_reminder (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id    VARCHAR(200) NOT NULL,
    claim_id    BIGINT       NOT NULL REFERENCES claim (id),
    reminded_on DATE         NOT NULL,
    CONSTRAINT claim_reminder_unique UNIQUE (sheet_id, claim_id, reminded_on)
);
