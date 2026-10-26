-- A claim's status: DRAFT when made; OPEN, then SETTLED; a DRAFT or OPEN claim can be VOID.
-- Only OPEN and SETTLED claims count toward the totals.
ALTER TABLE claim ADD COLUMN status VARCHAR(16) DEFAULT 'DRAFT' NOT NULL;
