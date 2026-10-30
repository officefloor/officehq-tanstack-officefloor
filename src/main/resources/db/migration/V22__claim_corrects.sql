-- A correction: a claim with a negative amount, naming by number the SETTLED claim of the same
-- person it corrects. Null for any other claim.
ALTER TABLE claim ADD COLUMN corrects BIGINT;
