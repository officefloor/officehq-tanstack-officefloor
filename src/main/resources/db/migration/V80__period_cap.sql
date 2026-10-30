-- A period may cap the number of OPEN or PENDING claims each person has in it (a whole number of at least 1);
-- NULL for none. Opening a claim of the period is refused when its person already has that many.
ALTER TABLE period ADD COLUMN claim_cap INTEGER;
