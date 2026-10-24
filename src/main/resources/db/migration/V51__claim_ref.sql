-- A claim's external reference (trimmed); null for a claim with none. Unique on its sheet, ignoring case.
ALTER TABLE claim ADD COLUMN ref VARCHAR(255);
