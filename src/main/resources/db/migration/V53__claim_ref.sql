-- A claim's external reference: unique on its sheet regardless of case; null when it has none.
ALTER TABLE claim ADD COLUMN ext_ref VARCHAR(200);
