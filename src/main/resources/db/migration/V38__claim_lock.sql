-- Opening a claim's editor locks it for other users (the user holding it; null: unlocked) until the
-- editor is saved or cancelled, or the sheet's today changes.
ALTER TABLE claim ADD COLUMN locked_by VARCHAR(200);
