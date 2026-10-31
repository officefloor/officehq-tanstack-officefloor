-- A claim's status: DRAFT when made, then OPEN, then SETTLED; a DRAFT or OPEN claim can go VOID.
-- Only OPEN and SETTLED claims count toward the totals.
ALTER TABLE claim ADD COLUMN status VARCHAR(10) DEFAULT 'DRAFT' NOT NULL;
