-- The last sheet-today a reminder was written for the claim (null: never reminded).
ALTER TABLE claim ADD COLUMN reminded_on DATE;
