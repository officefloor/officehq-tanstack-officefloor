-- Each policy version keeps the value its change set the setting to (null: blank), so the versions can be listed
-- as they were made even once a later change from the same date replaces the setting's value.
ALTER TABLE policy_version ADD COLUMN setting VARCHAR(20);

-- The versions already recorded take the value recorded for their setting from their from-date.
UPDATE policy_version v SET setting = (SELECT s.setting FROM setting_value s WHERE s.sheet_id = v.sheet_id
    AND s.setting_key = v.setting_key AND s.from_date = v.from_date);
