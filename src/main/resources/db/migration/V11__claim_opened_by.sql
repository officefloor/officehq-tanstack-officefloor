-- Who opened a claim (the signed-in user's name). NULL while a draft. They may not settle it.
ALTER TABLE claim ADD COLUMN opened_by VARCHAR(100);
