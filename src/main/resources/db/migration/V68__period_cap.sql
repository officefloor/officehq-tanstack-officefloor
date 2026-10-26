-- A period may cap the number of OPEN or PENDING claims each person has in it (by their due-by): null for none.
ALTER TABLE period ADD COLUMN claim_cap INT;
-- The sheet's snapshots, and their layout as of a date, carry it too.
ALTER TABLE hist.period ADD COLUMN claim_cap INT;
ALTER TABLE asof.period ADD COLUMN claim_cap INT;
