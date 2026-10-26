-- A steward may waive an OPEN or PENDING claim's late charge, once: a waived claim bears no late charge from then on.
ALTER TABLE claim ADD COLUMN waived BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE hist.claim ADD COLUMN waived BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE asof.claim ADD COLUMN waived BOOLEAN DEFAULT FALSE NOT NULL;
