-- The signed-in user who opened the claim; NULL until it is opened. That user may not settle it.
ALTER TABLE claim ADD COLUMN opened_by VARCHAR(255);
