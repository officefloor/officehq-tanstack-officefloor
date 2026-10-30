-- Who opened a claim (the signed-in user name); null for a claim never opened. Four eyes: the user
-- who opened a claim cannot settle it.
ALTER TABLE claim ADD COLUMN opened_by VARCHAR(255);
