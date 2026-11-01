-- The sheet's today on which an OPEN or PENDING claim was last reminded (null when never): a claim is reminded at
-- most once per sheet-today.
ALTER TABLE claim ADD COLUMN reminded_on DATE;
