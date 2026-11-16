-- A setting that carries a figure (clerk-limit, cap, floor, terms, terms-A/B/C, late-pct) is dated: each row is its
-- value from from_date, and the value in force on a date is the row with the latest from_date on or before it. An
-- undated setting (policy-date, period-numbering-from) has one row, from 0001-01-01; so do the values stored before
-- settings were dated (they keep applying to every date).
ALTER TABLE setting ADD COLUMN from_date DATE DEFAULT DATE '0001-01-01' NOT NULL;
ALTER TABLE setting DROP PRIMARY KEY;
ALTER TABLE setting ADD PRIMARY KEY (sheet_id, setting_key, from_date);
