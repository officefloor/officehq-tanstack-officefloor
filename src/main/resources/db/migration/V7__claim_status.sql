-- A claim's status: DRAFT (new), OPEN, SETTLED or VOID. Only OPEN and SETTLED claims count toward
-- the totals.
ALTER TABLE claim ADD COLUMN status VARCHAR(10) DEFAULT 'DRAFT' NOT NULL;
