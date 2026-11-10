-- A claim's status: DRAFT when made, then OPEN, then SETTLED; a DRAFT or OPEN claim may be VOID.
ALTER TABLE claim ADD COLUMN status VARCHAR(10) NOT NULL DEFAULT 'DRAFT';
