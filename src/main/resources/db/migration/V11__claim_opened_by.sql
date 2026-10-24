-- Who opened a claim: the signed-in name that opened it. Null while it was never opened. The one who
-- opened a claim cannot settle it.
ALTER TABLE claim ADD COLUMN opened_by VARCHAR(255);
