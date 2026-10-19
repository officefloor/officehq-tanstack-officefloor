-- A claim's status: DRAFT -> OPEN -> SETTLED, or DRAFT/OPEN -> VOID. SETTLED and VOID are final.
-- Only OPEN and SETTLED claims count toward the totals.
ALTER TABLE claim ADD COLUMN status VARCHAR(10) DEFAULT 'DRAFT' NOT NULL
    CHECK (status IN ('DRAFT', 'OPEN', 'SETTLED', 'VOID'));
