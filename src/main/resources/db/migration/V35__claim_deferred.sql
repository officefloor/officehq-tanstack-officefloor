-- The date an OPEN claim is deferred until; null when it was never deferred. Until the sheet's
-- today reaches it the claim counts in no total, is not overdue and cannot be settled.
ALTER TABLE claim ADD COLUMN deferred_until DATE;
