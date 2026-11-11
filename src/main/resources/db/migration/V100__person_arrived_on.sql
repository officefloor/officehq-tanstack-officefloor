-- The sheet's today when a person arrived on it by transfer (their latest transfer); null for anyone
-- never transferred.
ALTER TABLE person ADD COLUMN arrived_on DATE;
