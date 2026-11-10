-- The sheet's today when a user started watching a person (null: from the beginning of time).
ALTER TABLE person_watch ADD COLUMN watched_on DATE;
