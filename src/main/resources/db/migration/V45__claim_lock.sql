-- Who holds a claim's editor open: the signed-in user whose edit button locked it for everyone else,
-- until they save or cancel it or the sheet's today changes. Null when the claim is not locked.
ALTER TABLE claim ADD COLUMN locked_by VARCHAR(255);
