-- The sheet's today on which a person arrived on it by transfer (null for anyone who did not): a steward may undo the
-- transfer on that day only.
ALTER TABLE person ADD COLUMN arrived_on DATE;
