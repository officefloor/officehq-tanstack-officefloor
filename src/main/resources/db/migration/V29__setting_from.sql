-- A setting that carries a figure is dated: each value saved applies from its from-date, a key may
-- have several (one per from-date), and the value in force on a date is the one with the latest
-- from-date on or before it. No from-date applies from the beginning of time: the values set before
-- settings were dated, and the undated keys (policy-date, period-numbering-from).
ALTER TABLE sheet_setting ADD COLUMN from_date DATE;
ALTER TABLE sheet_setting DROP PRIMARY KEY;
ALTER TABLE sheet_setting ADD COLUMN from_key DATE GENERATED ALWAYS AS (COALESCE(from_date, DATE '0001-01-01'));
CREATE UNIQUE INDEX sheet_setting_from ON sheet_setting (sheet_id, setting, from_key);
