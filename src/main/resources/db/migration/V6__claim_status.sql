-- A claim's status: DRAFT -> OPEN -> SETTLED, or DRAFT/OPEN -> VOID. Existing claims start as drafts.
ALTER TABLE claim ADD COLUMN status VARCHAR(10) DEFAULT 'DRAFT' NOT NULL;
