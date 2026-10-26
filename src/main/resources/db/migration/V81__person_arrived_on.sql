-- The sheet's today on which a transferred person arrived on it; null for anyone not transferred in.
ALTER TABLE person ADD COLUMN arrived_on DATE;
-- The sheet's snapshots, and their layout as of a date, carry it too.
ALTER TABLE hist.person ADD COLUMN arrived_on DATE;
ALTER TABLE asof.person ADD COLUMN arrived_on DATE;
