-- The day each watch was started (the sheet's today then; NULL: before it was kept, so it counts on every date).
-- Read as of a date, a person counts as watched only by the watches started on or before it.
ALTER TABLE person_watch ADD COLUMN made_on DATE;
