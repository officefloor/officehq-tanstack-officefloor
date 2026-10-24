-- What each change was made on (the sheet's today then), so a statement can be taken as of a date: every change
-- made while the sheet's today was on or before that date counts, every later one does not. Null: made before
-- this was recorded, and counts as of any date.
ALTER TABLE person ADD COLUMN added_on DATE;
ALTER TABLE person ADD COLUMN retired_made_on DATE;
ALTER TABLE person_hold ADD COLUMN made_on DATE;
ALTER TABLE person_grade ADD COLUMN made_on DATE;
-- A statement's date is the one it was taken as of (the sheet's today when none was given); made_on is the
-- sheet's today when it was taken.
ALTER TABLE statement ADD COLUMN made_on DATE;
-- Every rate set, in the order set: replayed up to a date it gives the rates as they stood at the end of it.
CREATE TABLE rate_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL,
    grade VARCHAR(10) NOT NULL,
    pct DECIMAL(19, 2) NOT NULL,
    from_date DATE,
    to_date DATE,
    made_on DATE NOT NULL
);
CREATE INDEX rate_log_sheet ON rate_log (sheet_id, id);
-- Every setting set (dated, undated, the policy date), in the order set: replayed likewise.
CREATE TABLE setting_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL,
    setting_key VARCHAR(100) NOT NULL,
    from_date DATE,
    setting_value VARCHAR(200),
    made_on DATE NOT NULL
);
CREATE INDEX setting_log_sheet ON setting_log (sheet_id, id);
-- Every binding of a person's claims, in the order made: a statement's (the claims it bound, each with its figure)
-- or a cancel's (none bound). The last one made on or before a date gives the claims bound at the end of it.
CREATE TABLE binding_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL,
    person_id BIGINT NOT NULL,
    made_on DATE NOT NULL
);
CREATE INDEX binding_log_person ON binding_log (person_id, id);
CREATE TABLE binding_log_claim (
    binding_id BIGINT NOT NULL REFERENCES binding_log (id),
    claim_id BIGINT NOT NULL,
    stated_due DECIMAL(19, 2)
);
