-- The sheet's score log switch (on or blank; not dated). While it is on, changing the sheet's today records a SCORE
-- line per person, in list order, with their score on the day being left.
ALTER TABLE sheet ADD COLUMN score_log BOOLEAN NOT NULL DEFAULT FALSE;
