-- The sheet's score log switch (not dated). While on, changing the sheet's today audits each person's score on the day left.
ALTER TABLE sheet ADD COLUMN score_log BOOLEAN NOT NULL DEFAULT FALSE;
