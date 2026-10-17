-- Who opened a claim: the signed-in user, set when it is opened. None for a draft, or for claims
-- opened before this was recorded. The user who opened a claim cannot settle it.
ALTER TABLE claim ADD COLUMN opened_by VARCHAR(255);
