-- The user whose open editor locks the claim for everyone else; null when unlocked.
-- Released when that editor is saved or cancelled, and on every claim of the sheet when its today changes.
ALTER TABLE claim ADD COLUMN locked_by VARCHAR(200);
