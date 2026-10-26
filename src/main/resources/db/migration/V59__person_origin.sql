-- The sheet a transferred person came from; null for anyone not transferred in.
ALTER TABLE person ADD COLUMN origin VARCHAR(255);
-- The sheet's snapshots, and their layout as of a date, carry it too.
ALTER TABLE hist.person ADD COLUMN origin VARCHAR(255);
ALTER TABLE asof.person ADD COLUMN origin VARCHAR(255);
