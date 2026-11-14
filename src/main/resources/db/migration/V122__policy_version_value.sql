-- The value each policy version set its setting to (null: blank).
ALTER TABLE policy_version ADD COLUMN setting_value VARCHAR(100);
