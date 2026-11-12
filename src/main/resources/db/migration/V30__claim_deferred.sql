-- An OPEN claim deferred until a date after the sheet's today: until that day it counts in no total,
-- is not overdue, cannot be settled and is left unbound by a statement. Null when never deferred.
ALTER TABLE claim ADD COLUMN deferred_until DATE;
