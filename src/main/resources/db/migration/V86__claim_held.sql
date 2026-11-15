-- Whether an OPEN or PENDING claim is held: while open and not bound by a statement, it counts in no
-- live total (as a held person's claims do). Kept in its history for the sheet as of a date.
ALTER TABLE claim ADD COLUMN held BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE claim_history ADD COLUMN held BOOLEAN NOT NULL DEFAULT FALSE;
