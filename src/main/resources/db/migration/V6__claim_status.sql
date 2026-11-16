-- A claim's status: DRAFT when added; it may be opened (OPEN) and then settled (SETTLED), and a
-- DRAFT or OPEN claim may be voided (VOID). Only OPEN and SETTLED claims count toward the totals.
ALTER TABLE claim ADD COLUMN status VARCHAR(10) DEFAULT 'DRAFT' NOT NULL;
