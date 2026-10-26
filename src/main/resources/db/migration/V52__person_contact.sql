-- A person's contact: trimmed and lower-cased, null for none. No two people on a sheet share one.
ALTER TABLE person ADD COLUMN contact VARCHAR(255);
-- The sheet's snapshots, and their layout as of a date, carry it too.
ALTER TABLE hist.person ADD COLUMN contact VARCHAR(255);
ALTER TABLE asof.person ADD COLUMN contact VARCHAR(255);
