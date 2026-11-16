-- Who opened a claim: set when the claim is opened (DRAFT to OPEN) and kept from then on; null for
-- a claim never opened. The user who opened a claim cannot settle it (four eyes).
ALTER TABLE claim ADD COLUMN opened_by VARCHAR(200);
