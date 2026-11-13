-- A correction (a claim with a negative amount) names, by number, the settled claim of the same person it
-- corrects; null for any other claim.
ALTER TABLE claim ADD COLUMN corrects INT;
