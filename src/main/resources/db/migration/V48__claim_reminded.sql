-- The last day (the sheet's today then) a reminder was written for the claim. Null: never reminded.
ALTER TABLE claim ADD COLUMN reminded_on DATE;
