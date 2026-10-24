-- The number of the claim a correction corrects: a correction is a claim with a negative amount, allowed only
-- when it names a SETTLED claim of the same person. Null for any other claim.
ALTER TABLE claim ADD COLUMN corrects INT;
