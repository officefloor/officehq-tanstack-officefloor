-- The sheet's policy versions: every change to a policy setting (the rounding mode, the floor, the cap, the terms, the late
-- percent or the clerk limit) is a new version, numbered from 1 in the order made, with the setting changed and the date
-- it is in force from (null: none, it counts always).
CREATE TABLE policy_version (
    sheet_id VARCHAR(100) NOT NULL,
    number INT NOT NULL,
    setting_key VARCHAR(100) NOT NULL,
    from_date DATE
);
CREATE INDEX policy_version_sheet ON policy_version (sheet_id, number);
