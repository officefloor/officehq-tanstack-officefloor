-- The user who opened a claim (moved it from DRAFT to OPEN); null until it is opened. That user
-- may not settle it.
ALTER TABLE claim ADD COLUMN opened_by VARCHAR(255);
