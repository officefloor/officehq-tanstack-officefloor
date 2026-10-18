-- A claim with a negative amount is a correction: it names, by number, the SETTLED claim of the same
-- person that it corrects. None for any other claim.
ALTER TABLE claim ADD COLUMN corrects INT;
