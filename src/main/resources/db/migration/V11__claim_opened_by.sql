-- Who opened a claim: set when it is opened; none while a draft. The user who opened a claim
-- cannot settle it (four eyes).
ALTER TABLE claim ADD COLUMN opened_by VARCHAR(200);
