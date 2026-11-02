-- Each policy version also keeps the value its change set the setting to. A version kept before this takes the value
-- recorded for its setting under its from-date.
ALTER TABLE policy_version ADD COLUMN setting_value VARCHAR(4000) NOT NULL DEFAULT '';
UPDATE policy_version v SET setting_value = COALESCE((SELECT s.setting_value FROM setting s WHERE s.sheet = v.sheet
    AND s.setting_key = v.setting_key AND s.from_date = COALESCE(v.from_date, DATE '0001-01-01')), '');
