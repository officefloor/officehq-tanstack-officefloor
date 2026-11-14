-- The sheet's today when a person arrived on it by transfer; null: they were not transferred here.
ALTER TABLE person ADD COLUMN arrived_on DATE;
