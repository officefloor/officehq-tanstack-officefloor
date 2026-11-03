-- A sheet's score log switch (not dated): while on, changing the sheet's today writes one SCORE line per person
-- with their score as it stood on the day being left.
ALTER TABLE sheet ADD COLUMN score_log BOOLEAN NOT NULL DEFAULT FALSE;
