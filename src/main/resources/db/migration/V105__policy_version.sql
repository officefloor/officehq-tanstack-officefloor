-- The policy settings (the rounding mode, the floor, the cap, the terms, the late percent and the clerk limit) are
-- versioned together: one row per change to any of them, numbered in the order made (by id), with the from-date the
-- change is in force from (null for none: it counts always). A setting recorded before this was kept is a version with
-- the from-date it was recorded under (none for an undated one).
CREATE TABLE policy_version (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet       VARCHAR(200) NOT NULL,
    setting_key VARCHAR(100) NOT NULL,
    from_date   DATE
);
CREATE INDEX policy_version_sheet ON policy_version (sheet, id);
INSERT INTO policy_version (sheet, setting_key, from_date)
    SELECT sheet, setting_key, CASE WHEN from_date = DATE '0001-01-01' THEN NULL ELSE from_date END FROM setting
    WHERE setting_key IN ('rounding', 'floor', 'cap', 'terms', 'late-pct', 'clerk-limit')
    ORDER BY from_date, sheet, setting_key;
