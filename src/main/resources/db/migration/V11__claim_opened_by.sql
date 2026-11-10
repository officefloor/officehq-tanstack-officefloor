-- The signed-in user who opened a claim; null until it is opened. That user may not settle it.
ALTER TABLE claim ADD COLUMN opened_by VARCHAR(100);
