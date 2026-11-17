-- A claim's due-by may be set directly by a steward: it then stays as set, and a holiday added later does not work it
-- out again (see ClaimRepository#refixDueBys).
ALTER TABLE claim ADD COLUMN due_by_set BOOLEAN DEFAULT FALSE NOT NULL;
