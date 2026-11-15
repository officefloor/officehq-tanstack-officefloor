-- The last sheet's today an OPEN or PENDING claim was reminded on (none: never reminded); a claim can be
-- reminded once per sheet's today.
ALTER TABLE claim ADD COLUMN reminded_on DATE;
