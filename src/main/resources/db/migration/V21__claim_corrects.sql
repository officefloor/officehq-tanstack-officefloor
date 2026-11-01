-- A correction (a claim with a negative amount) names, by number, the settled claim of the same person it corrects.
ALTER TABLE claim ADD COLUMN corrects INTEGER;
