-- A setting that carries a figure (the floor, the late percent, the terms, the sheet's and each grade's, the clerk
-- limit and the cap) is dated: one value per sheet, key and from-date, the one in force on a date being the one of
-- the latest from-date on or before it. A setting that is not dated (the policy date, the period numbering
-- from-date) keeps its one row, from DATE '0001-01-01'; so does a value set before settings were dated, which
-- therefore stays in force from the beginning of time.
ALTER TABLE setting ADD COLUMN from_date DATE DEFAULT DATE '0001-01-01' NOT NULL;
ALTER TABLE setting DROP PRIMARY KEY;
ALTER TABLE setting ADD PRIMARY KEY (sheet, setting_key, from_date);
