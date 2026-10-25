-- An OPEN or PENDING claim can be reminded once per sheet-today: the last day a reminder was written (null: never).
ALTER TABLE claim ADD COLUMN reminded_on DATE;
ALTER TABLE hist.claim ADD COLUMN reminded_on DATE;
ALTER TABLE asof.claim ADD COLUMN reminded_on DATE;
