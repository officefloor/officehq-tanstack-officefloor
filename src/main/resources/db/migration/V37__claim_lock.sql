-- The user whose open editor locks the claim for every other user, until they save or cancel it or
-- the sheet's today changes. Null when it is not locked.
ALTER TABLE claim ADD COLUMN locked_by VARCHAR(255);
