-- The sheet's score log switch, not dated (false: off): while it is on, changing the sheet's today
-- writes a SCORE line per person, before the SNAPSHOT lines, with their score on the day being left.
ALTER TABLE sheet ADD COLUMN score_log BOOLEAN DEFAULT FALSE NOT NULL;
