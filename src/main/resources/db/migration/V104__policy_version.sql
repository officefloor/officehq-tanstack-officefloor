-- The policy settings (rounding, floor, cap, terms, late-pct and clerk-limit) are versioned together:
-- every change to one of them is a new version of the sheet's policy, numbered from 1 in the order made,
-- in force from the change's from-date (null: always).
CREATE TABLE policy_version (
    sheet_id   VARCHAR(200) NOT NULL REFERENCES sheet (id),
    version    INT          NOT NULL,
    setting    VARCHAR(100) NOT NULL,
    from_date  DATE,
    PRIMARY KEY (sheet_id, version)
);
-- The changes already made, in the order they were made.
INSERT INTO policy_version (sheet_id, version, setting, from_date)
    SELECT sheet_id, ROW_NUMBER() OVER (PARTITION BY sheet_id ORDER BY id), setting, from_date
    FROM setting_history
    WHERE setting IN ('rounding', 'floor', 'cap', 'terms', 'late-pct', 'clerk-limit');
