-- A sheet may name a parent sheet: an undated setting (key parent) whose value is a sheet's id, so the value is
-- widened to hold one (sheet ids are up to 200 long).
ALTER TABLE sheet_setting ALTER COLUMN setting_value VARCHAR(200);
