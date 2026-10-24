-- The date an OPEN claim is deferred until: while the sheet's today is before it the claim counts in no total, is not
-- overdue, cannot be settled and is left unbound by a statement. Null for a claim never deferred.
ALTER TABLE claim ADD COLUMN deferred_until DATE;
