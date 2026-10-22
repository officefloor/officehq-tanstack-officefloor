-- The last sheet-today a reminder was written for the claim (NULL: never reminded). A claim is reminded at most once
-- per sheet-today, and only while OPEN or PENDING.
ALTER TABLE claim ADD COLUMN reminded_on DATE;
ALTER TABLE claim_history ADD COLUMN reminded_on DATE;
