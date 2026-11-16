-- A claim may be deferred until a date: while the sheet's today is before it the claim counts in no total, is not
-- overdue, cannot be settled and is left unbound by a statement. Null: never deferred.
ALTER TABLE claim ADD COLUMN deferred_until DATE;
