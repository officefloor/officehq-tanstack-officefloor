-- An OPEN claim deferred until a date after the sheet's today (none: not deferred). Until the sheet's
-- today reaches that date it counts in no total, is not overdue, cannot be settled and is left unbound
-- by a statement; from that day on it is an OPEN claim like any other.
ALTER TABLE claim ADD COLUMN deferred_until DATE;
