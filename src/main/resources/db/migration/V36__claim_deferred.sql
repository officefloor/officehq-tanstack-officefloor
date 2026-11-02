-- The date an OPEN claim is deferred until (null when it was never deferred). Until the sheet's today
-- reaches it the claim counts in no total, is not overdue, cannot be settled and a statement leaves it
-- unbound; from that day on it is an OPEN claim like any other.
ALTER TABLE claim ADD COLUMN deferred_until DATE;
