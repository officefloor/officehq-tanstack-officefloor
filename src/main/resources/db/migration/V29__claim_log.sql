-- A claim's status log: one line per status it has had, oldest first (by id), each with the user
-- who made the change, the sheet's today then and the claim's due as computed at that moment
-- ('n/a' when it had none). A claim's first line is its DRAFT, written when it was added.
CREATE TABLE claim_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet(id),
    claim_id BIGINT NOT NULL REFERENCES claim(id),
    status VARCHAR(10) NOT NULL,
    by_user VARCHAR(200) NOT NULL,
    on_date DATE NOT NULL,
    due VARCHAR(30) NOT NULL
);
CREATE INDEX claim_log_sheet ON claim_log(sheet_id, claim_id, id);
