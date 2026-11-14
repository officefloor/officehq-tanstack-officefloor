-- The sheet's score log switch: while on, changing the sheet's today records each person's score as it stood. Not dated.
ALTER TABLE sheet ADD COLUMN score_log BOOLEAN DEFAULT FALSE NOT NULL;
