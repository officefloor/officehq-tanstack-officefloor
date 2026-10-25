-- The signed-in user who opened a claim; null while it was never opened. That user cannot settle it.
ALTER TABLE claim ADD COLUMN opened_by VARCHAR(200);
