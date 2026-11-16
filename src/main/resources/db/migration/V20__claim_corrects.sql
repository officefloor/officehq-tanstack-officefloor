-- A claim with a negative amount is a correction: the number of the SETTLED claim, of the same person, that it
-- corrects. Null for any other claim.
ALTER TABLE claim ADD COLUMN corrects BIGINT;
