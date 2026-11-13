-- The policy settings (rounding, floor, cap, terms, late-pct, clerk-limit) are versioned together: every change
-- to one of them is a new version of the sheet's policy, numbered from 1 in the order made. Each keeps the key and
-- value changed, the from-date it holds from (null for none: it counts always) and the sheet's today when it was
-- made (a version counts as of a date when made on or before it).
CREATE TABLE policy_version (
    sheet_id      VARCHAR(255) NOT NULL,
    version       INT          NOT NULL,
    setting_key   VARCHAR(64)  NOT NULL,
    setting_value VARCHAR(255),
    from_date     DATE,
    made_on       DATE         NOT NULL,
    PRIMARY KEY (sheet_id, version)
);
-- The policy values already recorded are the first versions, oldest from-date first; one recorded before settings
-- were dated has no from-date.
INSERT INTO policy_version (sheet_id, version, setting_key, setting_value, from_date, made_on)
    SELECT sheet_id, ROW_NUMBER() OVER (PARTITION BY sheet_id ORDER BY from_date, setting_key), setting_key,
        setting_value, CASEWHEN(from_date = DATE '0001-01-01', NULL, from_date), DATE '0001-01-01'
    FROM sheet_setting_from
    WHERE setting_key IN ('rounding', 'floor', 'cap', 'terms', 'late-pct', 'clerk-limit');
