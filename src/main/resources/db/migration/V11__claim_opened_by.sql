-- The user who opened a claim (four eyes: they cannot also settle it). Null until the claim is opened.
ALTER TABLE claim ADD COLUMN opened_by VARCHAR(255);
