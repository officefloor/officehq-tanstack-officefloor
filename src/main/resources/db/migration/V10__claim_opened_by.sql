-- Who opened the claim: the signed-in user named by the open. Null for a claim never opened.
-- The user who opened a claim cannot settle it.
ALTER TABLE claim ADD COLUMN opened_by VARCHAR(255);
