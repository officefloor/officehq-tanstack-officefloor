-- An OPEN claim may be deferred until a date after the sheet's today. Until the sheet's today
-- reaches it the claim counts in no total, is not overdue and cannot be settled; from that day on
-- it is an OPEN claim like any other. Null: never deferred.
ALTER TABLE claim ADD COLUMN deferred_until DATE;
