-- The sheet's policy versions: the policy settings (rounding, floor, cap, terms, late-pct, clerk-limit) are
-- versioned together. Every change to one of them is a new version, numbered from 1 in the order made, kept with
-- the setting changed and the from-date it was recorded from (null: none, a version that is in force always). The
-- sheet's current version is its number of versions; a claim's is the number whose from-date is on or before its date.
CREATE TABLE policy_version (
    sheet_id    VARCHAR(200) NOT NULL REFERENCES sheet (id),
    version     INT          NOT NULL,
    setting_key VARCHAR(20)  NOT NULL,
    from_date   DATE,
    PRIMARY KEY (sheet_id, version)
);

-- Each policy setting value already recorded is a version, oldest from-date first.
INSERT INTO policy_version (sheet_id, version, setting_key, from_date)
    SELECT sheet_id, ROW_NUMBER() OVER (PARTITION BY sheet_id ORDER BY from_date, setting_key), setting_key, from_date
    FROM setting_value WHERE setting_key IN ('rounding', 'floor', 'cap', 'terms', 'late-pct', 'clerk-limit');
