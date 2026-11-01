-- The user whose open editor locks a claim against everyone else's edit button: null when no editor holds it.
-- Released when that editor is saved or cancelled, and on every claim of the sheet when its today changes.
ALTER TABLE claim ADD COLUMN locked_by VARCHAR(200);
