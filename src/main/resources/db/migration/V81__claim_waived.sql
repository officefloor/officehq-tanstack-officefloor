-- Whether a steward waived the claim's late charge (while OPEN or PENDING): from then on it bears none, whatever its
-- overdue state.
ALTER TABLE claim ADD COLUMN waived BOOLEAN NOT NULL DEFAULT FALSE;
